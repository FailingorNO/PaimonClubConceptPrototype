package com.paimonclub.common;

import jakarta.enterprise.context.RequestScoped;

/**
 * 当前请求登录用户（由 AuthFilter 填充）。
 */
@RequestScoped
public class CurrentUser {

    private Long userId;
    private String username;
    private String role;
    private String moderatedCategories;

    public void fill(Long userId, String username, String role) {
        fill(userId, username, role, null);
    }

    public void fill(Long userId, String username, String role, String moderatedCategories) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.moderatedCategories = moderatedCategories;
    }

    public void clear() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.moderatedCategories = null;
    }

    /**
     * 我负责的板块 ID。
     * <p>返回空列表表示「不限板块」（管理员或全站版主）；非空表示只看这些板块。</p>
     */
    public java.util.List<Long> moderatedCategoryIds() {
        if (moderatedCategories == null || moderatedCategories.isBlank()) {
            return java.util.List.of();
        }
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String part : moderatedCategories.split(",")) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                // 脏数据跳过，不要让一个错误的配置把审核入口锁死
            }
        }
        return ids;
    }

    /** 是否只能审核指定板块 */
    public boolean isScopedModerator() {
        return !isAdmin() && !moderatedCategoryIds().isEmpty();
    }

    /** 校验是否有权处理某个板块的帖子 */
    public void assertCategoryScope(Long categoryId) {
        if (!isScopedModerator()) {
            return;
        }
        if (categoryId == null || !moderatedCategoryIds().contains(categoryId)) {
            throw BusinessException.forbidden("你只负责指定板块，无权处理该帖子");
        }
    }

    public boolean isLogin() {
        return userId != null;
    }

    /** 需要登录，否则抛 401 */
    public Long requireLogin() {
        if (userId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return userId;
    }

    public Long requireAdmin() {
        requireLogin();
        if (!Constants.ROLE_ADMIN.equals(role)) {
            throw BusinessException.forbidden("需要管理员权限");
        }
        return userId;
    }

    /** 管理员或版主 */
    public Long requireStaff() {
        requireLogin();
        if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_MODERATOR.equals(role)) {
            throw BusinessException.forbidden("需要版主及以上权限");
        }
        return userId;
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    public boolean isStaff() {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MODERATOR.equals(role);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
