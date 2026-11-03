package com.paimonclub.dao;

import com.paimonclub.entity.LikeRecord;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * 点赞 DAO。
 */
@ApplicationScoped
public class LikeRecordDao extends BaseDao<LikeRecord> {

    @Override
    protected Class<LikeRecord> entityClass() {
        return LikeRecord.class;
    }

    public boolean exists(Long userId, String targetType, Long targetId) {
        return count("select count(l) from LikeRecord l where l.userId = ?1 and l.targetType = ?2 and l.targetId = ?3",
                userId, targetType, targetId) > 0;
    }

    public int deleteOne(Long userId, String targetType, Long targetId) {
        return execute("delete from LikeRecord l where l.userId = ?1 and l.targetType = ?2 and l.targetId = ?3",
                userId, targetType, targetId);
    }

    public long countByTarget(String targetType, Long targetId) {
        return count("select count(l) from LikeRecord l where l.targetType = ?1 and l.targetId = ?2",
                targetType, targetId);
    }

    /** 帖子被彻底删除时清理点赞记录，避免悬空引用 */
    public int deleteByPost(Long postId) {
        return executeAndClear("delete from LikeRecord l where l.targetType = ?1 and l.targetId = ?2",
                "POST", postId);
    }

    /** 批量判断「我赞过哪些目标」，一次查询覆盖整页 */
    public java.util.Set<Long> likedTargetIds(Long userId, String targetType, java.util.Collection<Long> targetIds) {
        java.util.Set<Long> set = new java.util.HashSet<>();
        if (userId == null || targetIds == null || targetIds.isEmpty()) {
            return set;
        }
        // 标量投影必须显式声明结果类型，否则 JPA 会按实体类型转换而抛类型不匹配
        for (Object id : listOf(Object.class, "select l.targetId from LikeRecord l where l.userId = ?1 "
                + "and l.targetType = ?2 and l.targetId in ?3", userId, targetType, targetIds)) {
            if (id != null) {
                set.add(((Number) id).longValue());
            }
        }
        return set;
    }
}
