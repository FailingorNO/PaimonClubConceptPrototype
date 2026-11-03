package com.paimonclub.dao;

import com.paimonclub.common.Constants;
import com.paimonclub.entity.Post;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 帖子 DAO。
 */
@ApplicationScoped
public class PostDao extends BaseDao<Post> {

    @Override
    protected Class<Post> entityClass() {
        return Post.class;
    }

    public List<Post> page(Long categoryId, String keyword, String status, Long userId,
                           String sort, int first, int max) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(categoryId, keyword, status, userId, args);
        String order = "HOT".equalsIgnoreCase(sort)
                ? " order by p.top desc, (p.likeCount * 3 + p.commentCount * 2 + p.viewCount) desc, p.id desc"
                : " order by p.top desc, p.createdAt desc";
        return listPage("select p from Post p" + where + order, first, max, args.toArray());
    }

    public long countPage(Long categoryId, String keyword, String status, Long userId) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(categoryId, keyword, status, userId, args);
        return count("select count(p) from Post p" + where, args.toArray());
    }

    private String buildWhere(Long categoryId, String keyword, String status, Long userId, List<Object> args) {
        StringBuilder sb = new StringBuilder(" where 1 = 1");
        if (categoryId != null && categoryId > 0) {
            args.add(categoryId);
            sb.append(" and p.categoryId = ?").append(args.size());
        }
        if (keyword != null && !keyword.isBlank()) {
            args.add("%" + keyword.trim() + "%");
            sb.append(" and (p.title like ?").append(args.size());
            args.add("%" + keyword.trim() + "%");
            sb.append(" or p.content like ?").append(args.size()).append(")");
        }
        if (status != null && !status.isBlank()) {
            args.add(status);
            sb.append(" and p.status = ?").append(args.size());
        }
        if (userId != null) {
            args.add(userId);
            sb.append(" and p.userId = ?").append(args.size());
        }
        return sb.toString();
    }

    public void incrementView(Long postId) {
        execute("update Post p set p.viewCount = p.viewCount + 1 where p.id = ?1", postId);
    }

    public void increaseCommentCount(Long postId, int delta) {
        execute("update Post p set p.commentCount = p.commentCount + ?1 where p.id = ?2", delta, postId);
    }

    public List<Post> hot(int limit) {
        return listPage("select p from Post p where p.status = ?1 order by (p.likeCount * 3 + p.commentCount * 2 + p.viewCount) desc",
                0, limit, Constants.POST_PUBLISHED);
    }

    public long countByStatus(String status) {
        return count("select count(p) from Post p where p.status = ?1", status);
    }

    public long countAll() {
        return count("select count(p) from Post p");
    }

    public long countCreatedAfter(LocalDateTime since) {
        return count("select count(p) from Post p where p.createdAt >= ?1", since);
    }

    public long countByUser(Long userId) {
        return count("select count(p) from Post p where p.userId = ?1 and p.status <> ?2", userId, "DELETED");
    }

    /** 活跃作者排行：[userId, 发帖数] */
    public List<Object[]> topAuthors(LocalDateTime since, int limit) {
        return listOf(Object[].class,
                "select p.userId, count(p) from Post p where p.createdAt >= ?1 group by p.userId order by count(p) desc",
                0, limit, since);
    }

    /** 取指定时间之后的发布时间点，供业务层按天聚合（避免依赖具体数据库函数） */
    public List<java.time.LocalDateTime> listCreatedAfter(LocalDateTime since) {
        return listOf(java.time.LocalDateTime.class,
                "select p.createdAt from Post p where p.createdAt >= ?1 order by p.createdAt", since);
    }

    /** 批量累加浏览量：定时任务把一段时间的浏览合并成一次 UPDATE */
    public int incrementViewBy(Long postId, int delta) {
        return executeAndClear("update Post p set p.viewCount = p.viewCount + ?1 where p.id = ?2", delta, postId);
    }

    /** 某人已发布（审核通过）的帖子数，用于判断是不是「新号」 */
    public long countPublishedByUser(Long userId) {
        return count("select count(p) from Post p where p.userId = ?1 and p.status = ?2",
                userId, "PUBLISHED");
    }

    /** 待审帖子总数（限定板块） */
    public long countPendingInCategories(java.util.Collection<Long> categoryIds) {
        return count("select count(p) from Post p where p.status = ?1 and p.categoryId in ?2",
                "PENDING", categoryIds);
    }

    /** 待审帖子分页（限定板块） */
    public List<Post> pagePendingInCategories(java.util.Collection<Long> categoryIds, int first, int max) {
        return listPage("select p from Post p where p.status = ?1 and p.categoryId in ?2 order by p.id asc",
                first, max, "PENDING", categoryIds);
    }
}
