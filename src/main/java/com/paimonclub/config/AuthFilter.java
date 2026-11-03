package com.paimonclub.config;

import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.JwtUtil;
import com.paimonclub.entity.User;
import com.paimonclub.service.AuthCacheService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JWT 鉴权过滤器：解析 {@code Authorization: Bearer <token>}，把当前用户注入 {@link CurrentUser}。
 *
 * <p>与早期版本的关键差别：令牌只是「身份线索」，真正的账号状态、角色一律以数据库为准。
 * 因此管理员封禁或降权后，用户手里的旧令牌在 5 秒内即失效——封禁不再是摆设。</p>
 *
 * <p>解析失败、用户不存在、账号被封禁时，本过滤器不注入身份，后续写操作由 Service 层的
 * {@code requireLogin()} / {@code requireStaff()} 拦下（返回 401/403）。</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    @Inject
    CurrentUser currentUser;

    @Inject
    AuthCacheService authCacheService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        currentUser.clear();
        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return;
        }
        Claims claims;
        try {
            claims = JwtUtil.parse(token);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "JWT 解析失败: {0}", ex.getMessage());
            return;
        }
        Long userId;
        try {
            userId = JwtUtil.userId(claims);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "JWT 载荷异常: {0}", ex.getMessage());
            return;
        }
        User user;
        try {
            user = authCacheService.load(userId);
        } catch (Exception ex) {
            // 数据库不可用时不能让鉴权过滤器把整个应用打挂：降级为仅签名校验，并记录告警
            LOG.log(Level.WARNING, "鉴权查库失败，降级为仅校验令牌签名: " + ex.getMessage(), ex);
            currentUser.fill(userId,
                    claims.get("username", String.class),
                    claims.get("role", String.class));
            return;
        }
        if (user == null) {
            LOG.log(Level.FINE, "令牌对应的用户已不存在: {0}", userId);
            return;
        }
        if (user.getStatus() == null || user.getStatus() != Constants.USER_NORMAL) {
            LOG.log(Level.INFO, "账号已被封禁，拒绝注入身份: {0}", userId);
            return;
        }
        // 角色与负责板块都以数据库为准：降权、撤销版主授权都能即时生效
        currentUser.fill(userId, user.getUsername(), user.getRole(), user.getModeratedCategories());
    }
}
