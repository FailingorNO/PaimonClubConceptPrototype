package com.paimonclub.dao;

import com.paimonclub.entity.Favorite;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 收藏 DAO。
 */
@ApplicationScoped
public class FavoriteDao extends BaseDao<Favorite> {

    @Override
    protected Class<Favorite> entityClass() {
        return Favorite.class;
    }

    public boolean exists(Long userId, Long postId) {
        return count("select count(f) from Favorite f where f.userId = ?1 and f.postId = ?2", userId, postId) > 0;
    }

    public int deleteOne(Long userId, Long postId) {
        return execute("delete from Favorite f where f.userId = ?1 and f.postId = ?2", userId, postId);
    }

    public List<Favorite> pageByUser(Long userId, int first, int max) {
        return listPage("select f from Favorite f where f.userId = ?1 order by f.id desc", first, max, userId);
    }

    public long countByUser(Long userId) {
        return count("select count(f) from Favorite f where f.userId = ?1", userId);
    }

    public long countByPost(Long postId) {
        return count("select count(f) from Favorite f where f.postId = ?1", postId);
    }

    /** 帖子被彻底删除时清理收藏记录 */
    public int deleteByPost(Long postId) {
        return executeAndClear("delete from Favorite f where f.postId = ?1", postId);
    }

    /** 批量判断「我收藏了哪些帖子」 */
    public java.util.Set<Long> favoritedPostIds(Long userId, java.util.Collection<Long> postIds) {
        java.util.Set<Long> set = new java.util.HashSet<>();
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return set;
        }
        for (Object id : listOf(Object.class,
                "select f.postId from Favorite f where f.userId = ?1 and f.postId in ?2", userId, postIds)) {
            if (id != null) {
                set.add(((Number) id).longValue());
            }
        }
        return set;
    }
}
