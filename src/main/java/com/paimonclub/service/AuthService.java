package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.JwtUtil;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.User;
import com.paimonclub.util.PasswordUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务：注册、登录、令牌签发。
 */
@ApplicationScoped
public class AuthService {

    @Inject
    UserDao userDao;
    @Inject
    VoMapper mapper;
    @Inject
    LoginGuard loginGuard;

    @Transactional
    public Vos.LoginResult register(Reqs.Register req) {
        String username = req.username.trim();
        if (userDao.existsByUsername(username)) {
            throw BusinessException.badRequest("用户名已被占用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(req.password));
        String nickname = req.nickname == null || req.nickname.isBlank() ? username : req.nickname.trim();
        user.setNickname(XssUtil.sanitize(nickname));
        user.setAvatarUrl(Constants.DEFAULT_AVATAR);
        user.setEmail(req.email);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.USER_NORMAL);
        user.setPoints(Constants.POINTS_WELCOME);
        user.setBio("这位旅行者还没有填写签名");
        user.setCreatedAt(LocalDateTime.now());
        userDao.persist(user);
        return buildResult(user);
    }

    /** 兼容旧调用（无客户端 IP） */
    public Vos.LoginResult login(Reqs.Login req) {
        return login(req, null);
    }

    /**
     * 登录：带失败限流。
     *
     * @param clientIp 客户端 IP，用于按来源限流（可为空）
     */
    public Vos.LoginResult login(Reqs.Login req, String clientIp) {
        String guardKey = loginGuard.key(req.username, clientIp);
        loginGuard.ensureAllowed(guardKey);

        User user = userDao.findByUsername(req.username.trim());
        if (user == null || !PasswordUtil.matches(req.password, user.getPasswordHash())) {
            loginGuard.onFailure(guardKey);
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == Constants.USER_BANNED) {
            throw BusinessException.forbidden("账号已被封禁，请联系管理员");
        }
        loginGuard.onSuccess(guardKey);
        return buildResult(user);
    }

    private Vos.LoginResult buildResult(User user) {
        Vos.LoginResult result = new Vos.LoginResult();
        result.token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        result.user = mapper.toUser(user, user.getId());
        return result;
    }
}
