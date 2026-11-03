package com.paimonclub.rest;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.CommentService;
import com.paimonclub.service.InteractionService;
import com.paimonclub.service.ReportService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 评论接口：新增、楼中楼、删除、点赞、举报。
 */
@Path("/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    @Inject CommentService commentService;
    @Inject InteractionService interactionService;
    @Inject ReportService reportService;
    @Inject CurrentUser currentUser;

    @POST
    public Result<Vos.Comment> add(@Valid Reqs.Comment req) {
        return Result.ok(commentService.add(req));
    }

    @GET
    @Path("/post/{postId}")
    public Result<List<Vos.Comment>> tree(@PathParam("postId") Long postId) {
        return Result.ok(commentService.tree(postId));
    }

    @DELETE
    @Path("/{id}")
    public Result<Void> delete(@PathParam("id") Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @POST
    @Path("/{id}/like")
    public Result<Boolean> like(@PathParam("id") Long id) {
        return Result.ok(interactionService.toggleLike("COMMENT", id));
    }

    @POST
    @Path("/{id}/report")
    public Result<Void> report(@PathParam("id") Long id, @Valid Reqs.Report req) {
        req.targetType = "COMMENT";
        req.targetId = id;
        reportService.submit(req);
        return Result.ok();
    }
}
