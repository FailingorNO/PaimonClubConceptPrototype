package com.paimonclub.dao;

import com.paimonclub.entity.Comment;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 评论 DAO。
 */
@ApplicationScoped
public class CommentDao extends BaseDao<Comment> {

    @Override
    protected Class<Comment> entityClass() {
        return Comment.class;
    }

    public List<Comment> listByPost(Long postId) {
        return list("select c from Comment c where c.postId = ?1 and c.status = 1 order by c.id asc", postId);
    }

    public long countByPost(Long postId) {
        return count("select count(c) from Comment c where c.postId = ?1 and c.status = 1", postId);
    }

    public long countAll() {
        return count("select count(c) from Comment c where c.status = 1");
    }

    public long countCreatedAfter(java.time.LocalDateTime since) {
        return count("select count(c) from Comment c where c.createdAt >= ?1 and c.status = 1", since);
    }

    public long countByUser(Long userId) {
        return count("select count(c) from Comment c where c.userId = ?1 and c.status = 1", userId);
    }

    /** 帖子软删时把评论一起隐藏 */
    public int softDeleteByPost(Long postId) {
        return executeAndClear("update Comment c set c.status = 0 where c.postId = ?1 and c.status = 1", postId);
    }

    /** 软删单条评论，返回影响行数（已删过则返回 0，天然幂等） */
    public int softDeleteOne(Long commentId) {
        return executeAndClear("update Comment c set c.status = 0 where c.id = ?1 and c.status = 1", commentId);
    }

    /** 根评论被删除时，把整楼的回复一起隐藏，避免出现孤儿回复 */
    public int softDeleteReplies(Long rootId) {
        return executeAndClear("update Comment c set c.status = 0 where c.rootId = ?1 and c.status = 1", rootId);
    }
}
