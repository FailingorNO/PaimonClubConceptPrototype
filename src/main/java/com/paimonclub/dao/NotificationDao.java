package com.paimonclub.dao;

import com.paimonclub.entity.Notification;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 通知 DAO。
 */
@ApplicationScoped
public class NotificationDao extends BaseDao<Notification> {

    @Override
    protected Class<Notification> entityClass() {
        return Notification.class;
    }

    public List<Notification> pageByUser(Long userId, int first, int max) {
        return listPage("select n from Notification n where n.userId = ?1 order by n.id desc", first, max, userId);
    }

    public long countByUser(Long userId) {
        return count("select count(n) from Notification n where n.userId = ?1", userId);
    }

    public long countUnread(Long userId) {
        return count("select count(n) from Notification n where n.userId = ?1 and n.isRead = false", userId);
    }

    public int markAllRead(Long userId) {
        return execute("update Notification n set n.isRead = true where n.userId = ?1 and n.isRead = false", userId);
    }

    /** 找一条可合并的未读通知（同一个人、同一类型、同一目标） */
    public Notification findMergeable(Long userId, String type, Long refId) {
        List<Notification> list = list("select n from Notification n where n.userId = ?1 and n.type = ?2 "
                + "and n.isRead = false and n.refId = ?3 order by n.id desc", userId, type, refId);
        return list.isEmpty() ? null : list.get(0);
    }
}
