package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.AuditRecordDao;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.AuditRecord;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审核服务：帖子审核（先审后发/先发后审）、置顶、强制下架、审核记录。
 */
@ApplicationScoped
public class AuditService {

    @Inject PostDao postDao;
    @Inject AuditRecordDao auditRecordDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject LocalCacheService cache;

    @Transactional
    public void audit(Reqs.Audit req) {
        Long auditorId = currentUser.requireStaff();
        String result = req.result == null ? "" : req.result.toUpperCase();
        if (!"APPROVED".equals(result) && !"REJECTED".equals(result)) {
            throw BusinessException.badRequest("审核结果只能是 APPROVED 或 REJECTED");
        }
        if (Constants.TARGET_POST.equalsIgnoreCase(req.targetType == null ? "" : req.targetType)) {
            Post post = postDao.findRequired(req.targetId);
            currentUser.assertCategoryScope(post.getCategoryId());
            if ("APPROVED".equals(result)) {
                post.setStatus(Constants.POST_PUBLISHED);
                post.setRejectReason(null);
                postDao.update(post);
                pointsService.award(post.getUserId(), Constants.POINTS_POST_CREATE,
                        Constants.POINT_TYPE_POST_CREATE, post.getId(), "帖子审核通过");
                notificationService.push(post.getUserId(), Constants.NOTIFY_AUDIT,
                        "你的帖子《" + post.getTitle() + "》已通过审核", post.getId());
            } else {
                post.setStatus(Constants.POST_REJECTED);
                post.setRejectReason(req.reason);
                postDao.update(post);
                notificationService.push(post.getUserId(), Constants.NOTIFY_AUDIT,
                        "你的帖子《" + post.getTitle() + "》未通过审核：" + (req.reason == null ? "不符合社区规范" : req.reason),
                        post.getId());
            }
        }

        AuditRecord record = new AuditRecord();
        record.setTargetType(req.targetType == null ? Constants.TARGET_POST : req.targetType.toUpperCase());
        record.setTargetId(req.targetId);
        record.setAuditorId(auditorId);
        record.setResult(result);
        record.setReason(req.reason);
        record.setCreatedAt(LocalDateTime.now());
        auditRecordDao.persist(record);
        cache.evictPrefix("posts:hot");
    }

    public PageResult<Vos.Audit> records(int page, int size) {
        currentUser.requireStaff();
        long total = auditRecordDao.countAll();
        List<Vos.Audit> items = new ArrayList<>();
        for (AuditRecord record : auditRecordDao.pageAll((page - 1) * size, size)) {
            Vos.Audit vo = new Vos.Audit();
            vo.id = record.getId();
            vo.targetType = record.getTargetType();
            vo.targetId = record.getTargetId();
            vo.result = record.getResult();
            vo.reason = record.getReason();
            vo.createdAt = DateUtil.format(record.getCreatedAt());
            User auditor = userDao.find(record.getAuditorId());
            vo.auditorName = auditor == null ? "系统" : auditor.getNickname();
            if (Constants.TARGET_POST.equals(record.getTargetType())) {
                Post post = postDao.find(record.getTargetId());
                vo.targetTitle = post == null ? "已删除的帖子" : post.getTitle();
            }
            items.add(vo);
        }
        return PageResult.of(total, page, size, items);
    }

    /** 置顶/取消置顶 */
    @Transactional
    public void setTop(Long postId, boolean top) {
        currentUser.requireStaff();
        Post post = postDao.findRequired(postId);
        currentUser.assertCategoryScope(post.getCategoryId());
        post.setTop(top);
        postDao.update(post);
        cache.evictPrefix("posts:hot");
    }

    /** 管理端强制下架 */
    @Transactional
    public void takeDown(Long postId, String reason) {
        currentUser.requireStaff();
        Post post = postDao.findRequired(postId);
        currentUser.assertCategoryScope(post.getCategoryId());
        post.setStatus(Constants.POST_REJECTED);
        post.setRejectReason(reason == null ? "违反社区规范，已被管理员下架" : reason);
        postDao.update(post);
        notificationService.push(post.getUserId(), Constants.NOTIFY_AUDIT,
                "你的帖子《" + post.getTitle() + "》已被下架：" + post.getRejectReason(), postId);
        cache.evictPrefix("posts:hot");
    }
}
