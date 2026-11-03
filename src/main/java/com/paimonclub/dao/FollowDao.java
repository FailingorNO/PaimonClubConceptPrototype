package com.paimonclub.dao;

import com.paimonclub.entity.Follow;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 关注 DAO。
 */
@ApplicationScoped
public class FollowDao extends BaseDao<Follow> {

    @Override
    protected Class<Follow> entityClass() {
        return Follow.class;
    }

    public boolean exists(Long followerId, Long followeeId) {
        return count("select count(f) from Follow f where f.followerId = ?1 and f.followeeId = ?2",
                followerId, followeeId) > 0;
    }

    public int deleteOne(Long followerId, Long followeeId) {
        return execute("delete from Follow f where f.followerId = ?1 and f.followeeId = ?2",
                followerId, followeeId);
    }

    /** 我关注的人 */
    public List<Long> followeeIds(Long followerId) {
        return listOf(Long.class, "select f.followeeId from Follow f where f.followerId = ?1", followerId);
    }

    /** 关注我的人 */
    public List<Long> followerIds(Long followeeId) {
        return listOf(Long.class, "select f.followerId from Follow f where f.followeeId = ?1", followeeId);
    }

    public long countFollowers(Long userId) {
        return count("select count(f) from Follow f where f.followeeId = ?1", userId);
    }

    public long countFollowees(Long userId) {
        return count("select count(f) from Follow f where f.followerId = ?1", userId);
    }
}
