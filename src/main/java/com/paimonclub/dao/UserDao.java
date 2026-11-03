package com.paimonclub.dao;

import com.paimonclub.entity.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 DAO。
 */
@ApplicationScoped
public class UserDao extends BaseDao<User> {

    @Override
    protected Class<User> entityClass() {
        return User.class;
    }

    public User findByUsername(String username) {
        return single("select u from User u where u.username = ?1", username);
    }

    public boolean existsByUsername(String username) {
        return count("select count(u) from User u where u.username = ?1", username) > 0;
    }

    public List<User> page(String keyword, String role, Integer status, int first, int max) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(keyword, role, status, args);
        return listPage("select u from User u" + where + " order by u.id desc", first, max, args.toArray());
    }

    public long countPage(String keyword, String role, Integer status) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(keyword, role, status, args);
        return count("select count(u) from User u" + where, args.toArray());
    }

    private String buildWhere(String keyword, String role, Integer status, List<Object> args) {
        StringBuilder sb = new StringBuilder(" where 1 = 1");
        if (keyword != null && !keyword.isBlank()) {
            args.add("%" + keyword.trim() + "%");
            sb.append(" and (u.username like ?").append(args.size());
            args.add("%" + keyword.trim() + "%");
            sb.append(" or u.nickname like ?").append(args.size()).append(")");
        }
        if (role != null && !role.isBlank()) {
            args.add(role);
            sb.append(" and u.role = ?").append(args.size());
        }
        if (status != null) {
            args.add(status);
            sb.append(" and u.status = ?").append(args.size());
        }
        return sb.toString();
    }

    public long countCreatedAfter(LocalDateTime since) {
        return count("select count(u) from User u where u.createdAt >= ?1", since);
    }

    public long countAll() {
        return count("select count(u) from User u");
    }

    public List<User> topByPoints(int limit) {
        return listPage("select u from User u where u.status = 1 order by u.points desc", 0, limit);
    }

    /**
     * 原子增加积分（允许负数，但保证余额不会被减成负数）。
     *
     * @return 受影响行数，0 表示用户不存在或扣减后余额为负
     */
    public int addPoints(Long userId, int delta) {
        if (userId == null || delta == 0) {
            return 0;
        }
        if (delta > 0) {
            return executeAndClear("update User u set u.points = u.points + ?1 where u.id = ?2", delta, userId);
        }
        return executeAndClear(
                "update User u set u.points = u.points + ?1 where u.id = ?2 and u.points + ?1 >= 0", delta, userId);
    }

    /**
     * 原子扣减积分：把「先查余额再扣」的两步合并成一条带条件的 UPDATE，
     * 避免并发下两个请求都通过余额校验、最终扣成负数。
     *
     * @return 受影响行数，0 表示余额不足或用户不存在
     */
    public int deductPoints(Long userId, int amount) {
        if (userId == null || amount <= 0) {
            return 0;
        }
        return executeAndClear(
                "update User u set u.points = u.points - ?1 where u.id = ?2 and u.points >= ?1", amount, userId);
    }

    /**
     * 原子标记签到：只有「今天还没签」时才会更新成功。
     *
     * @return 1 表示本次签到有效，0 表示今天已签过
     */
    public int markSignIn(Long userId, java.time.LocalDate today) {
        return executeAndClear(
                "update User u set u.lastSignInDate = ?1 where u.id = ?2 and (u.lastSignInDate is null or u.lastSignInDate < ?1)",
                today, userId);
    }

    public List<User> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list("select u from User u where u.id in ?1", (Object) ids);
    }

    /** 批量按 ID 取用户，列表映射时避免逐条查库 */
    public java.util.Map<Long, User> mapByIds(java.util.Collection<Long> ids) {
        java.util.Map<Long, User> map = new java.util.HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (User user : list("select u from User u where u.id in ?1", ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }
}
