package com.paimonclub.dao;

import com.paimonclub.entity.Order;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 兑换订单 DAO。
 */
@ApplicationScoped
public class OrderDao extends BaseDao<Order> {

    @Override
    protected Class<Order> entityClass() {
        return Order.class;
    }

    public List<Order> pageByUser(Long userId, int first, int max) {
        return listPage("select o from Order o where o.userId = ?1 order by o.id desc", first, max, userId);
    }

    public long countByUser(Long userId) {
        return count("select count(o) from Order o where o.userId = ?1", userId);
    }

    public List<Order> pageAll(int first, int max) {
        return listPage("select o from Order o order by o.id desc", first, max);
    }

    public long countAll() {
        return count("select count(o) from Order o");
    }

    public long sumPointsCost() {
        List<Object> values = listOf(Object.class, "select sum(o.pointsCost) from Order o");
        if (values.isEmpty() || values.get(0) == null) {
            return 0L;
        }
        return ((Number) values.get(0)).longValue();
    }
}
