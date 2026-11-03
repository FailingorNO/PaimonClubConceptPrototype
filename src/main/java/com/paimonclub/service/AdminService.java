package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.CommentDao;
import com.paimonclub.dao.OrderDao;
import com.paimonclub.dao.PointsRecordDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.ReportDao;
import com.paimonclub.dao.SysConfigDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.SysConfig;
import com.paimonclub.entity.User;
import com.paimonclub.util.SensitiveWordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理服务：数据看板、用户管理、系统配置、敏感词维护。
 */
@ApplicationScoped
public class AdminService {

    @Inject UserDao userDao;
    @Inject PostDao postDao;
    @Inject CommentDao commentDao;
    @Inject ReportDao reportDao;
    @Inject OrderDao orderDao;
    @Inject PointsRecordDao pointsRecordDao;
    @Inject SysConfigDao configDao;
    @Inject CurrentUser currentUser;
    @Inject VoMapper mapper;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject AuthCacheService authCacheService;

    /** 数据看板 */
    public Vos.Stats stats() {
        currentUser.requireStaff();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Vos.Stats stats = new Vos.Stats();
        stats.userCount = userDao.countAll();
        stats.postCount = postDao.countAll();
        stats.commentCount = commentDao.countAll();
        stats.todayUsers = userDao.countCreatedAfter(todayStart);
        stats.todayPosts = postDao.countCreatedAfter(todayStart);
        stats.pendingPosts = postDao.countByStatus(Constants.POST_PENDING);
        stats.pendingReports = reportDao.countByStatus(Constants.REPORT_PENDING);
        stats.orderCount = orderDao.countAll();
        stats.totalPointsGranted = pointsRecordDao.sumGranted();
        stats.totalPointsConsumed = Math.abs(pointsRecordDao.sumConsumed());

        // 近 7 天发帖趋势
        Map<String, Long> byDay = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            byDay.put(start.plusDays(i).toString(), 0L);
        }
        for (LocalDateTime time : postDao.listCreatedAfter(start.atStartOfDay())) {
            String key = time.toLocalDate().toString();
            if (byDay.containsKey(key)) {
                byDay.put(key, byDay.get(key) + 1);
            }
        }
        for (Map.Entry<String, Long> entry : byDay.entrySet()) {
            stats.postTrend.add(new Vos.DayCount(entry.getKey(), entry.getValue()));
        }
        for (User user : userDao.topByPoints(5)) {
            stats.topUsers.add(mapper.toUser(user));
        }
        return stats;
    }

    public PageResult<Vos.User> users(String keyword, String role, Integer status, int page, int size) {
        currentUser.requireStaff();
        long total = userDao.countPage(keyword, role, status);
        List<Vos.User> items = new ArrayList<>();
        for (User user : userDao.page(keyword, role, status, (page - 1) * size, size)) {
            Vos.User vo = mapper.toUser(user);
            // 后台需要看到版主授权范围，便于调整；公开接口不会返回该字段
            vo.moderatedCategories = user.getModeratedCategories();
            items.add(vo);
        }
        return PageResult.of(total, page, size, items);
    }

    @Transactional
    public Vos.User updateUser(Long userId, Reqs.AdminUser req) {
        Long adminId = currentUser.requireAdmin();
        User user = userDao.findRequired(userId);
        if (userId.equals(adminId) && req.status != null && req.status == Constants.USER_BANNED) {
            throw BusinessException.badRequest("不能封禁自己");
        }
        if (req.role != null && !req.role.isBlank()) {
            if (!Constants.ROLE_USER.equals(req.role) && !Constants.ROLE_MODERATOR.equals(req.role)
                    && !Constants.ROLE_ADMIN.equals(req.role)) {
                throw BusinessException.badRequest("非法角色");
            }
            user.setRole(req.role);
        }
        if (req.moderatedCategories != null) {
            user.setModeratedCategories(normalizeCategories(req.moderatedCategories));
        }
        if (req.status != null) {
            user.setStatus(req.status);
            notificationService.push(userId, Constants.NOTIFY_SYSTEM,
                    req.status == Constants.USER_BANNED ? "你的账号已被封禁" : "你的账号已恢复正常", null);
        }
        userDao.update(user);
        // 立即失效鉴权缓存：封禁、解封、升降权必须在下一个请求就生效，而不是等 5 秒
        authCacheService.evict(userId);
        if (req.pointsDelta != null && req.pointsDelta != 0) {
            pointsService.award(userId, req.pointsDelta, Constants.POINT_TYPE_ADMIN, null,
                    req.reason == null ? "管理员调整" : req.reason);
        }
        return mapper.toUser(userDao.findRequired(userId));
    }

    /** 系统配置列表 */
    public List<Map<String, Object>> configs() {
        currentUser.requireStaff();
        List<Map<String, Object>> items = new ArrayList<>();
        for (SysConfig config : configDao.listAll()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", config.getCfgKey());
            map.put("value", config.getCfgValue());
            map.put("remark", config.getRemark());
            items.add(map);
        }
        return items;
    }

    @Transactional
    public void updateConfig(Reqs.Config req) {
        currentUser.requireAdmin();
        configDao.put(req.key, req.value, null);
    }

    /** 新增敏感词，立即生效 */
    @Transactional
    public void addSensitiveWord(String word) {
        currentUser.requireAdmin();
        if (word == null || word.isBlank()) {
            throw BusinessException.badRequest("敏感词不能为空");
        }
        SensitiveWordUtil.addWord(word.trim());
    }

    /** 归一化板块配置：只保留数字 ID，去重后用逗号连接 */
    private String normalizeCategories(String raw) {
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(text));
            } catch (NumberFormatException ex) {
                throw BusinessException.badRequest("板块 ID 必须是数字：" + text);
            }
        }
        if (ids.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }
}
