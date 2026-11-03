package com.paimonclub.dao;

import com.paimonclub.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分商品 DAO。
 */
@ApplicationScoped
public class ProductDao extends BaseDao<Product> {

    @Override
    protected Class<Product> entityClass() {
        return Product.class;
    }

    public List<Product> listOnSale() {
        return list("select p from Product p where p.status = 1 order by p.pricePoints asc");
    }

    public List<Product> page(String keyword, String type, Integer status, int first, int max) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(keyword, type, status, args);
        return listPage("select p from Product p" + where + " order by p.id desc", first, max, args.toArray());
    }

    public long countPage(String keyword, String type, Integer status) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(keyword, type, status, args);
        return count("select count(p) from Product p" + where, args.toArray());
    }

    private String buildWhere(String keyword, String type, Integer status, List<Object> args) {
        StringBuilder sb = new StringBuilder(" where 1 = 1");
        if (keyword != null && !keyword.isBlank()) {
            args.add("%" + keyword.trim() + "%");
            sb.append(" and p.name like ?").append(args.size());
        }
        if (type != null && !type.isBlank()) {
            args.add(type);
            sb.append(" and p.type = ?").append(args.size());
        }
        if (status != null) {
            args.add(status);
            sb.append(" and p.status = ?").append(args.size());
        }
        return sb.toString();
    }

    /**
     * 原子扣库存：只有库存大于 0 时才会扣减成功。
     *
     * @return 1 表示扣减成功，0 表示库存已被抢空
     */
    public int decreaseStock(Long productId) {
        return executeAndClear("update Product p set p.stock = p.stock - 1 where p.id = ?1 and p.stock > 0", productId);
    }

    /** 取消订单时恢复库存 */
    public int increaseStock(Long productId) {
        return executeAndClear("update Product p set p.stock = p.stock + 1 where p.id = ?1", productId);
    }
}
