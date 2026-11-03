package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.PointsRecordDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.PointsRecord;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 积分服务：所有积分变动必须经过这里，保证流水与余额一致。
 *
 * <p>并发安全靠数据库原子操作，而不是「先查余额再判断」：
 * 扣减走 {@code UPDATE ... SET points = points - ? WHERE points >= ?}，
 * 由数据库的行锁保证同一用户的两个并发请求不会同时通过余额校验。</p>
 *
 * <p>每条流水都写入 {@code balanceAfter} 快照，对账时可以直接核对，
 * 不必从第一条流水开始累加。</p>
 */
@ApplicationScoped
public class PointsService {

    @Inject
    UserDao userDao;
    @Inject
    PointsRecordDao recordDao;

    /** 发放积分，返回实际到账值 */
    @Transactional
    public int award(Long userId, int amount, String type, Long refId, String description) {
        if (userId == null || amount == 0) {
            return 0;
        }
        int affected = userDao.addPoints(userId, amount);
        if (affected == 0) {
            // 用户不存在，或扣减会减成负数
            return 0;
        }
        User user = userDao.find(userId);
        int balance = user == null || user.getPoints() == null ? 0 : user.getPoints();
        writeRecord(userId, amount, type, refId, description, balance);
        return amount;
    }

    /**
     * 扣减积分，余额不足抛业务异常。
     *
     * <p>整条 UPDATE 是原子的：并发场景下只有一个请求能把余额从 6 扣到 1，
     * 另一个请求的 WHERE 条件不成立，影响行数为 0，从而正确抛出「积分不足」。</p>
     */
    @Transactional
    public void spend(Long userId, int amount, String type, Long refId, String description) {
        if (amount <= 0) {
            throw BusinessException.badRequest("扣减积分必须为正数");
        }
        int affected = userDao.deductPoints(userId, amount);
        if (affected == 0) {
            User user = userDao.find(userId);
            if (user == null) {
                throw BusinessException.notFound("用户不存在");
            }
            throw BusinessException.badRequest("积分不足，当前积分：" + (user.getPoints() == null ? 0 : user.getPoints()));
        }
        User user = userDao.find(userId);
        int balance = user == null || user.getPoints() == null ? 0 : user.getPoints();
        writeRecord(userId, -amount, type, refId, description, balance);
    }

    private void writeRecord(Long userId, int amount, String type, Long refId, String description, int balanceAfter) {
        if (amount == 0) {
            return;
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType(type);
        record.setRefId(refId);
        record.setDescription(description);
        record.setBalanceAfter(balanceAfter);
        record.setCreatedAt(LocalDateTime.now());
        recordDao.persist(record);
    }

    public PageResult<Vos.PointsRecord> page(Long userId, int page, int size) {
        long total = recordDao.countByUser(userId);
        List<PointsRecord> records = recordDao.pageByUser(userId, (page - 1) * size, size);
        List<Vos.PointsRecord> items = new ArrayList<>();
        for (PointsRecord record : records) {
            Vos.PointsRecord vo = new Vos.PointsRecord();
            vo.id = record.getId();
            vo.amount = record.getAmount();
            vo.balanceAfter = record.getBalanceAfter();
            vo.type = record.getType();
            vo.description = record.getDescription();
            vo.createdAt = DateUtil.format(record.getCreatedAt());
            items.add(vo);
        }
        return PageResult.of(total, page, size, items);
    }
}
