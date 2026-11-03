package com.paimonclub.dao;

import com.paimonclub.entity.PointsRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 积分流水 DAO。
 */
@ApplicationScoped
public class PointsRecordDao extends BaseDao<PointsRecord> {

    @Override
    protected Class<PointsRecord> entityClass() {
        return PointsRecord.class;
    }

    public List<PointsRecord> pageByUser(Long userId, int first, int max) {
        return listPage("select r from PointsRecord r where r.userId = ?1 order by r.id desc", first, max, userId);
    }

    public long countByUser(Long userId) {
        return count("select count(r) from PointsRecord r where r.userId = ?1", userId);
    }

    public long countAll() {
        return count("select count(r) from PointsRecord r");
    }

    public long sumAmountByType(String type) {
        List<Object> values = listOf(Object.class,
                "select sum(r.amount) from PointsRecord r where r.type = ?1", type);
        if (values.isEmpty() || values.get(0) == null) {
            return 0L;
        }
        return ((Number) values.get(0)).longValue();
    }

    /** 累计发放（正向流水总和），系统统计口径 */
    public long sumGranted() {
        return sumWhere("r.amount > 0");
    }

    /** 累计消耗（负向流水绝对值总和） */
    public long sumConsumed() {
        return sumWhere("r.amount < 0");
    }

    private long sumWhere(String condition) {
        List<Object> values = listOf(Object.class,
                "select sum(r.amount) from PointsRecord r where " + condition);
        if (values.isEmpty() || values.get(0) == null) {
            return 0L;
        }
        return ((Number) values.get(0)).longValue();
    }

    /** 某人在某类积分上的当日获得次数，用于限制「刷分」式重复领奖 */
    public long countToday(Long userId, String type) {
        return count("select count(r) from PointsRecord r where r.userId = ?1 and r.type = ?2 and r.createdAt >= ?3",
                userId, type, java.time.LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
