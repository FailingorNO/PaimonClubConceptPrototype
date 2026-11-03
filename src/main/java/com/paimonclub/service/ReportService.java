package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.PostDao;
import com.paimonclub.dao.ReportDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Post;
import com.paimonclub.entity.Report;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 举报服务：用户举报、管理端处理（含下架处置）。
 */
@ApplicationScoped
public class ReportService {

    @Inject ReportDao reportDao;
    @Inject PostDao postDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject NotificationService notificationService;
    @Inject AuditService auditService;

    @Transactional
    public void submit(Reqs.Report req) {
        Long userId = currentUser.requireLogin();
        if (Constants.TARGET_POST.equalsIgnoreCase(req.targetType)) {
            postDao.findRequired(req.targetId);
        }
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(req.targetType.toUpperCase());
        report.setTargetId(req.targetId);
        report.setReason(XssUtil.sanitize(req.reason.trim()));
        report.setStatus(Constants.REPORT_PENDING);
        report.setCreatedAt(LocalDateTime.now());
        reportDao.persist(report);
    }

    public PageResult<Vos.Report> page(String status, int page, int size) {
        currentUser.requireStaff();
        long total = reportDao.countPage(status);
        List<Vos.Report> items = new ArrayList<>();
        for (Report report : reportDao.page(status, (page - 1) * size, size)) {
            Vos.Report vo = new Vos.Report();
            vo.id = report.getId();
            vo.reporterId = report.getReporterId();
            User reporter = userDao.find(report.getReporterId());
            vo.reporterName = reporter == null ? "未知用户" : reporter.getNickname();
            vo.targetType = report.getTargetType();
            vo.targetId = report.getTargetId();
            vo.reason = report.getReason();
            vo.status = report.getStatus();
            vo.handleResult = report.getHandleResult();
            vo.createdAt = DateUtil.format(report.getCreatedAt());
            if (Constants.TARGET_POST.equals(report.getTargetType())) {
                Post post = postDao.find(report.getTargetId());
                vo.targetSummary = post == null ? "帖子已删除" : "《" + post.getTitle() + "》";
            }
            items.add(vo);
        }
        return PageResult.of(total, page, size, items);
    }

    /** 处理举报：result=HANDLED（成立）/ REJECTED（不成立） */
    @Transactional
    public void handle(Long reportId, Reqs.Audit req) {
        Long handlerId = currentUser.requireStaff();
        Report report = reportDao.findRequired(reportId);
        if (!Constants.REPORT_PENDING.equals(report.getStatus())) {
            throw BusinessException.badRequest("该举报已处理");
        }
        String result = req.result == null ? "HANDLED" : req.result.toUpperCase();
        report.setStatus("REJECTED".equals(result) ? Constants.REPORT_REJECTED : Constants.REPORT_HANDLED);
        report.setHandlerId(handlerId);
        report.setHandleResult(XssUtil.sanitize(req.reason == null ? "" : req.reason));
        reportDao.update(report);

        if (Boolean.TRUE.equals(req.takeDown) && Constants.TARGET_POST.equals(report.getTargetType())) {
            auditService.takeDown(report.getTargetId(), req.reason);
        }
        notificationService.push(report.getReporterId(), Constants.NOTIFY_SYSTEM,
                "你的举报已处理：" + report.getHandleResult(), reportId);
    }

    public long countPending() {
        return reportDao.countByStatus(Constants.REPORT_PENDING);
    }
}
