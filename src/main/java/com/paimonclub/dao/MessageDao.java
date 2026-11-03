package com.paimonclub.dao;

import com.paimonclub.entity.Message;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 私信 DAO。
 */
@ApplicationScoped
public class MessageDao extends BaseDao<Message> {

    @Override
    protected Class<Message> entityClass() {
        return Message.class;
    }

    public List<Message> listConversation(Long a, Long b, int limit) {
        return list("select m from Message m where (m.fromUser = ?1 and m.toUser = ?2) or (m.fromUser = ?2 and m.toUser = ?1) order by m.id desc",
                0, limit, a, b);
    }

    public List<Message> listInbox(Long userId, int limit) {
        return listPage("select m from Message m where m.toUser = ?1 order by m.id desc", 0, limit, userId);
    }

    public long countUnread(Long userId) {
        return count("select count(m) from Message m where m.toUser = ?1 and m.isRead = false", userId);
    }

    /** 兼容旧调用：把该会话全部未读标记为已读 */
    public int markRead(Long fromUser, Long toUser) {
        return markRead(fromUser, toUser, null);
    }

    /**
     * 标记已读，带时间上界。
     *
     * <p>只把「进入会话那一刻之前」到达的消息标为已读：用户停留在会话页期间新收到的消息
     * 不会被顺手吞掉，红点和未读数才是可信的。</p>
     */
    public int markRead(Long fromUser, Long toUser, java.time.LocalDateTime upTo) {
        if (upTo == null) {
            return execute("update Message m set m.isRead = true where m.fromUser = ?1 and m.toUser = ?2 and m.isRead = false",
                    fromUser, toUser);
        }
        return execute("update Message m set m.isRead = true where m.fromUser = ?1 and m.toUser = ?2"
                        + " and m.isRead = false and m.createdAt <= ?3",
                fromUser, toUser, upTo);
    }
}
