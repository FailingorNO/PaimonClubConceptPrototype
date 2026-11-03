package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.OrderDao;
import com.paimonclub.dao.ProductDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Order;
import com.paimonclub.entity.Product;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 积分商城服务：商品浏览、积分兑换、订单履约、商品管理。
 *
 * <p>订单不是「扣分记录」：实物商品会经历 待发货 → 已发货 → 已签收，
 * 待发货阶段可取消并原路退还积分、恢复库存。</p>
 */
@ApplicationScoped
public class MallService {

    @Inject ProductDao productDao;
    @Inject OrderDao orderDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;

    public PageResult<Vos.Product> page(String keyword, String type, int page, int size) {
        long total = productDao.countPage(keyword, type, 1);
        List<Product> products = productDao.page(keyword, type, 1, (page - 1) * size, size);
        List<Vos.Product> items = new ArrayList<>();
        for (Product product : products) {
            items.add(toProductVo(product));
        }
        return PageResult.of(total, page, size, items);
    }

    public List<Vos.Product> onSale() {
        List<Vos.Product> items = new ArrayList<>();
        for (Product product : productDao.listOnSale()) {
            items.add(toProductVo(product));
        }
        return items;
    }

    @Transactional
    public Vos.Order exchange(Reqs.Exchange req) {
        Long userId = currentUser.requireLogin();
        Product product = productDao.findRequired(req.productId);
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw BusinessException.badRequest("商品已下架");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw BusinessException.badRequest("商品库存不足");
        }

        boolean physical = Constants.PRODUCT_PHYSICAL.equalsIgnoreCase(product.getType());
        if (physical) {
            if (isBlank(req.receiverName) || isBlank(req.receiverPhone) || isBlank(req.receiverAddress)) {
                throw BusinessException.badRequest("实物商品需要填写收货人、联系电话和收货地址");
            }
        }

        pointsService.spend(userId, product.getPricePoints(), Constants.POINT_TYPE_EXCHANGE,
                product.getId(), "兑换商品：" + product.getName());

        // 扣库存用带条件的原子 UPDATE，避免并发超卖
        if (productDao.decreaseStock(product.getId()) == 0) {
            throw BusinessException.badRequest("商品库存不足");
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductType(product.getType());
        order.setPointsCost(product.getPricePoints());
        order.setCreatedAt(now);
        if (physical) {
            order.setStatus(Constants.ORDER_CREATED);
            order.setReceiverName(XssUtil.sanitize(req.receiverName.trim()));
            order.setReceiverPhone(req.receiverPhone.trim());
            order.setReceiverAddress(XssUtil.sanitize(req.receiverAddress.trim()));
            order.setRemark(req.remark == null ? null : XssUtil.sanitize(req.remark.trim()));
        } else {
            // 虚拟商品当场发放，直接完成
            order.setStatus(Constants.ORDER_COMPLETED);
            order.setFinishedAt(now);
        }
        orderDao.persist(order);

        String tip = physical
                ? "兑换成功：" + product.getName() + "，等待管理员发货"
                : "兑换成功：" + product.getName() + "，权益已发放";
        notificationService.push(userId, Constants.NOTIFY_POINTS, tip, order.getId());
        return toOrderVo(order);
    }

    public PageResult<Vos.Order> myOrders(int page, int size) {
        Long userId = currentUser.requireLogin();
        long total = orderDao.countByUser(userId);
        List<Vos.Order> items = new ArrayList<>();
        for (Order order : orderDao.pageByUser(userId, (page - 1) * size, size)) {
            items.add(toOrderVo(order));
        }
        return PageResult.of(total, page, size, items);
    }

    /** 管理端：全部订单 */
    public PageResult<Vos.Order> allOrders(int page, int size) {
        currentUser.requireAdmin();
        long total = orderDao.countAll();
        List<Vos.Order> items = new ArrayList<>();
        for (Order order : orderDao.pageAll((page - 1) * size, size)) {
            items.add(toOrderVo(order));
        }
        return PageResult.of(total, page, size, items);
    }

    /** 管理员发货：填写物流单号，订单进入已发货 */
    @Transactional
    public Vos.Order ship(Long orderId, String logisticsNo) {
        currentUser.requireStaff();
        if (isBlank(logisticsNo)) {
            throw BusinessException.badRequest("物流单号不能为空");
        }
        Order order = orderDao.findRequired(orderId);
        if (!Constants.ORDER_CREATED.equals(order.getStatus())) {
            throw BusinessException.badRequest("只有待发货订单可以发货");
        }
        order.setLogisticsNo(XssUtil.sanitize(logisticsNo.trim()));
        order.setStatus(Constants.ORDER_SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderDao.update(order);
        notificationService.push(order.getUserId(), Constants.NOTIFY_POINTS,
                "你的兑换「" + order.getProductName() + "」已发货，物流单号：" + order.getLogisticsNo(), orderId);
        return toOrderVo(order);
    }

    /** 用户确认收货 */
    @Transactional
    public Vos.Order finish(Long orderId) {
        Long userId = currentUser.requireLogin();
        Order order = orderDao.findRequired(orderId);
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("只能确认自己的订单");
        }
        if (!Constants.ORDER_SHIPPED.equals(order.getStatus())) {
            throw BusinessException.badRequest("订单当前状态不能确认收货");
        }
        order.setStatus(Constants.ORDER_COMPLETED);
        order.setFinishedAt(LocalDateTime.now());
        orderDao.update(order);
        return toOrderVo(order);
    }

    /** 取消待发货订单：退还积分并恢复库存 */
    @Transactional
    public Vos.Order cancel(Long orderId) {
        Long userId = currentUser.requireLogin();
        Order order = orderDao.findRequired(orderId);
        boolean owner = userId.equals(order.getUserId());
        if (!owner && !currentUser.isStaff()) {
            throw BusinessException.forbidden("只能取消自己的订单");
        }
        if (!Constants.ORDER_CREATED.equals(order.getStatus())) {
            throw BusinessException.badRequest("只有待发货订单可以取消");
        }
        order.setStatus(Constants.ORDER_CANCELED);
        order.setFinishedAt(LocalDateTime.now());
        orderDao.update(order);

        pointsService.award(order.getUserId(), order.getPointsCost(), Constants.POINT_TYPE_EXCHANGE_REFUND,
                order.getId(), "取消订单退还：" + order.getProductName());
        productDao.increaseStock(order.getProductId());
        notificationService.push(order.getUserId(), Constants.NOTIFY_POINTS,
                "订单「" + order.getProductName() + "」已取消，积分已退还", orderId);
        return toOrderVo(order);
    }

    @Transactional
    public Vos.Product save(Reqs.Product req) {
        currentUser.requireAdmin();
        Product product = req.id == null ? new Product() : productDao.findRequired(req.id);
        product.setName(XssUtil.sanitize(req.name.trim()));
        product.setDescription(XssUtil.sanitize(req.description == null ? "" : req.description.trim()));
        product.setImage(req.image);
        product.setPricePoints(req.pricePoints);
        product.setStock(req.stock);
        product.setType(req.type);
        product.setStatus(req.status == null ? 1 : req.status);
        if (product.getId() == null) {
            product.setCreatedAt(LocalDateTime.now());
            productDao.persist(product);
        } else {
            productDao.update(product);
        }
        return toProductVo(product);
    }

    @Transactional
    public void delete(Long productId) {
        currentUser.requireAdmin();
        productDao.removeById(productId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Vos.Product toProductVo(Product product) {
        Vos.Product vo = new Vos.Product();
        vo.id = product.getId();
        vo.name = product.getName();
        vo.description = product.getDescription();
        vo.image = product.getImage();
        vo.pricePoints = product.getPricePoints();
        vo.stock = product.getStock();
        vo.type = product.getType();
        vo.status = product.getStatus();
        vo.createdAt = DateUtil.format(product.getCreatedAt());
        return vo;
    }

    private Vos.Order toOrderVo(Order order) {
        Vos.Order vo = new Vos.Order();
        vo.id = order.getId();
        vo.productId = order.getProductId();
        vo.productName = order.getProductName();
        vo.productType = order.getProductType();
        vo.pointsCost = order.getPointsCost();
        vo.status = order.getStatus();
        vo.receiverName = order.getReceiverName();
        vo.receiverPhone = order.getReceiverPhone();
        vo.receiverAddress = order.getReceiverAddress();
        vo.logisticsNo = order.getLogisticsNo();
        vo.remark = order.getRemark();
        vo.shippedAt = DateUtil.format(order.getShippedAt());
        vo.finishedAt = DateUtil.format(order.getFinishedAt());
        vo.createdAt = DateUtil.format(order.getCreatedAt());
        return vo;
    }
}
