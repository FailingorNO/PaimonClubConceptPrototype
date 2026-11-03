package com.paimonclub.dao;

import com.paimonclub.entity.Friend;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 好友 DAO。
 */
@ApplicationScoped
public class FriendDao extends BaseDao<Friend> {

    @Override
    protected Class<Friend> entityClass() {
        return Friend.class;
    }

    /**
     * 取两人之间的关系记录。
     * <p>
     * 历史数据里可能同时存在 A→B 和 B→A 两条，这里取最早一条而不是 getSingleResult，
     * 避免 NonUniqueResultException 直接把接口打成 500。
     */
    public Friend findBetween(Long a, Long b) {
        List<Friend> list = list("select f from Friend f where (f.userId = ?1 and f.friendId = ?2) "
                + "or (f.userId = ?2 and f.friendId = ?1) order by f.id asc", a, b);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 删除两个方向的所有关系记录，避免残留记录让后续状态判断错乱 */
    public int deleteBetween(Long a, Long b) {
        return executeAndClear("delete from Friend f where (f.userId = ?1 and f.friendId = ?2) "
                + "or (f.userId = ?2 and f.friendId = ?1)", a, b);
    }

    /** 任一方向存在拉黑，即视为不可通信 */
    public boolean isBlocked(Long a, Long b) {
        return count("select count(f) from Friend f where ((f.userId = ?1 and f.friendId = ?2) "
                + "or (f.userId = ?2 and f.friendId = ?1)) and f.status = ?3", a, b, "BLOCKED") > 0;
    }

    /** 我主动拉黑的人（只看我这个方向，不把「拉黑我的人」算进我的黑名单） */
    public List<Friend> listMyBlocked(Long userId) {
        return list("select f from Friend f where f.userId = ?1 and f.status = ?2 order by f.id desc",
                userId, "BLOCKED");
    }

    /** 我的好友（双向） */
    public List<Friend> listByStatus(Long userId, String status) {
        return list("select f from Friend f where (f.userId = ?1 or f.friendId = ?1) and f.status = ?2 order by f.id desc",
                userId, status);
    }

    /** 别人发给我的申请 */
    public List<Friend> listIncoming(Long userId, String status) {
        return list("select f from Friend f where f.friendId = ?1 and f.status = ?2 order by f.id desc",
                userId, status);
    }
}
