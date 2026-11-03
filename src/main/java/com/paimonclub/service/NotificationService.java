package com.paimonclub.service;

import com.paimonclub.common.Constants;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.NotificationDao;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Notification;
import com.paimonclub.util.DateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通知服务：系统通知与互动通知。
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    @Inject
    NotificationDao notificationDao;

    /** 可以合并的通知类型：同一个人反复点赞/评论/关注时只保留一条并累加次数 */
    private static boolean mergeable(String type) {
        return Constants.NOTIFY_LIKE.equals(type)
                || Constants.NOTIFY_COMMENT.equals(type)
                || Constants.NOTIFY_FOLLOW.equals(type);
    }

    @Transactional
    public void push(Long userId, String type, String content, Long refId) {
        if (userId == null) {
            return;
        }
        try {
            String text = content == null ? "" : content.substring(0, Math.min(content.length(), 250));
            if (mergeable(type)) {
                Notification exist = notificationDao.findMergeable(userId, type, refId);
                if (exist != null) {
                    // 合并到已有未读通知：刷新内容与时间，累加合并次数
                    exist.setContent(text);
                    exist.setCreatedAt(LocalDateTime.now());
                    int count = exist.getMergeCount() == null ? 1 : exist.getMergeCount();
                    exist.setMergeCount(count + 1);
                    notificationDao.update(exist);
                    return;
                }
            }
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setContent(text);
            notification.setRefId(refId);
            notification.setIsRead(false);
            notification.setMergeCount(1);
            notification.setCreatedAt(LocalDateTime.now());
            notificationDao.persist(notification);
        } catch (Exception ex) {
            // 通知失败不影响主业务
            LOG.log(Level.WARNING, "写入通知失败", ex);
        }
    }

    public PageResult<Vos.Notification> page(Long userId, int page, int size) {
        long total = notificationDao.countByUser(userId);
        List<Notification> list = notificationDao.pageByUser(userId, (page - 1) * size, size);
        List<Vos.Notification> items = new ArrayList<>();
        for (Notification n : list) {
            Vos.Notification vo = new Vos.Notification();
            vo.id = n.getId();
            vo.type = n.getType();
            vo.content = n.getContent();
            vo.refId = n.getRefId();
            vo.isRead = n.getIsRead();
            vo.mergeCount = n.getMergeCount() == null ? 1 : n.getMergeCount();
            vo.createdAt = DateUtil.format(n.getCreatedAt());
            items.add(vo);
        }
        return PageResult.of(total, page, size, items);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationDao.markAllRead(userId);
    }

    public long unread(Long userId) {
        return userId == null ? 0 : notificationDao.countUnread(userId);
    }
}
