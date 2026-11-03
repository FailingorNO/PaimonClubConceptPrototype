package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.User;
import com.paimonclub.util.PasswordUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务：资料、密码、签到、用户搜索。
 */
@ApplicationScoped
public class UserService {

    @Inject
    UserDao userDao;
    @Inject VoMapper mapper;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;

    public Vos.User me() {
        Long userId = currentUser.requireLogin();
        User user = userDao.findRequired(userId);
        Vos.User vo = mapper.toUser(user, userId);
        vo.unreadNotifications = (int) notificationService.unread(userId);
        return vo;
    }

    public Vos.User profile(Long userId) {
        User user = userDao.findRequired(userId);
        return mapper.toUser(user, currentUser.getUserId());
    }

    @Transactional
    public Vos.User updateProfile(Reqs.Profile req) {
        Long userId = currentUser.requireLogin();
        User user = userDao.findRequired(userId);
        if (req.nickname != null && !req.nickname.isBlank()) {
            user.setNickname(XssUtil.sanitize(req.nickname.trim()));
        }
        if (req.avatarUrl != null && !req.avatarUrl.isBlank()) {
            user.setAvatarUrl(req.avatarUrl.trim());
        }
        if (req.bio != null) {
            user.setBio(XssUtil.sanitize(req.bio.trim()));
        }
        if (req.email != null) {
            user.setEmail(req.email.trim());
        }
        if (req.phone != null) {
            user.setPhone(req.phone.trim());
        }
        userDao.update(user);
        return mapper.toUser(user, userId);
    }

    @Transactional
    public void changePassword(Reqs.Password req) {
        Long userId = currentUser.requireLogin();
        User user = userDao.findRequired(userId);
        if (!PasswordUtil.matches(req.oldPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        user.setPasswordHash(PasswordUtil.hash(req.newPassword));
        userDao.update(user);
    }

    /**
     * 每日签到。
     *
     * <p>用「带条件的原子 UPDATE」判定今天是否已签：只有 last_sign_in_date 早于今天时才会更新成功。
     * 用户连点两次、或两个请求同时到达，也只有一次能拿到影响行数 1，不会重复领积分。</p>
     */
    @Transactional
    public Vos.SignIn signIn() {
        Long userId = currentUser.requireLogin();
        Vos.SignIn vo = new Vos.SignIn();
        LocalDate today = LocalDate.now();

        int affected = userDao.markSignIn(userId, today);
        if (affected == 0) {
            User user = userDao.findRequired(userId);
            vo.signed = false;
            vo.gain = 0;
            vo.totalPoints = user.getPoints();
            vo.message = "今天已经签到过了，明天再来吧";
            return vo;
        }

        int gain = pointsService.award(userId, Constants.POINTS_SIGN_IN, Constants.POINT_TYPE_SIGN_IN, null, "每日签到");
        User latest = userDao.findRequired(userId);
        vo.signed = true;
        vo.gain = gain;
        vo.totalPoints = latest.getPoints();
        vo.message = "签到成功，获得 " + gain + " 积分";
        return vo;
    }

    public PageResult<Vos.User> search(String keyword, int page, int size) {
        long total = userDao.countPage(keyword, null, Constants.USER_NORMAL);
        List<User> users = userDao.page(keyword, null, Constants.USER_NORMAL, (page - 1) * size, size);
        List<Vos.User> items = new ArrayList<>();
        for (User user : users) {
            items.add(mapper.toUser(user, currentUser.getUserId()));
        }
        return PageResult.of(total, page, size, items);
    }

    public List<Vos.User> pointsRanking(int limit) {
        List<Vos.User> items = new ArrayList<>();
        for (User user : userDao.topByPoints(limit)) {
            items.add(mapper.toUser(user));
        }
        return items;
    }
}
