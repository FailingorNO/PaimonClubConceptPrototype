package com.paimonclub.rest;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.AdminService;
import com.paimonclub.service.AuditService;
import com.paimonclub.service.PostService;
import com.paimonclub.service.ReportService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * 后台管理接口：看板、用户管理、内容审核、举报处理、配置维护。
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject AdminService adminService;
    @Inject AuditService auditService;
    @Inject ReportService reportService;
    @Inject PostService postService;
    @Inject CurrentUser currentUser;

    @GET
    @Path("/stats")
    public Result<Vos.Stats> stats() {
        currentUser.requireStaff();
        return Result.ok(adminService.stats());
    }

    @GET
    @Path("/users")
    public Result<PageResult<Vos.User>> users(@QueryParam("keyword") String keyword,
                                              @QueryParam("role") String role,
                                              @QueryParam("status") Integer status,
                                              @QueryParam("page") @DefaultValue("1") int page,
                                              @QueryParam("size") @DefaultValue("10") int size) {
        currentUser.requireStaff();
        return Result.ok(adminService.users(keyword, role, status, page, size));
    }

    @PUT
    @Path("/users/{id}")
    public Result<Vos.User> updateUser(@PathParam("id") Long id, Reqs.AdminUser req) {
        currentUser.requireAdmin();
        return Result.ok(adminService.updateUser(id, req));
    }

    @GET
    @Path("/audits/pending")
    public Result<PageResult<Vos.Post>> pending(@QueryParam("page") @DefaultValue("1") int page,
                                                @QueryParam("size") @DefaultValue("10") int size) {
        currentUser.requireStaff();
        return Result.ok(postService.pending(page, size));
    }

    @POST
    @Path("/audits")
    public Result<Void> audit(@Valid Reqs.Audit req) {
        currentUser.requireStaff();
        auditService.audit(req);
        return Result.ok();
    }

    @GET
    @Path("/audits/records")
    public Result<PageResult<Vos.Audit>> auditRecords(@QueryParam("page") @DefaultValue("1") int page,
                                                      @QueryParam("size") @DefaultValue("10") int size) {
        currentUser.requireStaff();
        return Result.ok(auditService.records(page, size));
    }

    @POST
    @Path("/posts/{id}/top")
    public Result<Void> setTop(@PathParam("id") Long id,
                               @QueryParam("top") @DefaultValue("true") boolean top) {
        currentUser.requireStaff();
        auditService.setTop(id, top);
        return Result.ok();
    }

    @POST
    @Path("/posts/{id}/takedown")
    public Result<Void> takeDown(@PathParam("id") Long id, Map<String, String> body) {
        currentUser.requireStaff();
        String reason = body == null ? null : body.get("reason");
        auditService.takeDown(id, reason);
        return Result.ok();
    }

    @GET
    @Path("/reports")
    public Result<PageResult<Vos.Report>> reports(@QueryParam("status") String status,
                                                  @QueryParam("page") @DefaultValue("1") int page,
                                                  @QueryParam("size") @DefaultValue("10") int size) {
        currentUser.requireStaff();
        return Result.ok(reportService.page(status, page, size));
    }

    @POST
    @Path("/reports/{id}/handle")
    public Result<Void> handleReport(@PathParam("id") Long id, Reqs.Audit req) {
        currentUser.requireStaff();
        reportService.handle(id, req);
        return Result.ok();
    }

    @GET
    @Path("/configs")
    public Result<List<Map<String, Object>>> configs() {
        currentUser.requireStaff();
        return Result.ok(adminService.configs());
    }

    @PUT
    @Path("/configs")
    public Result<Void> updateConfig(@Valid Reqs.Config req) {
        currentUser.requireStaff();
        adminService.updateConfig(req);
        return Result.ok();
    }

    @POST
    @Path("/sensitive-words")
    public Result<Void> addSensitiveWord(Map<String, String> body) {
        currentUser.requireStaff();
        String word = body == null ? null : body.get("word");
        if (word == null || word.isBlank()) {
            throw BusinessException.badRequest("敏感词不能为空");
        }
        adminService.addSensitiveWord(word.trim());
        return Result.ok();
    }
}
