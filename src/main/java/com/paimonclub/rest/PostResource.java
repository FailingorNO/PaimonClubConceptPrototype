package com.paimonclub.rest;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.common.Result;
import com.paimonclub.common.SiteConfig;
import com.paimonclub.dao.PostCategoryDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.PostCategory;
import com.paimonclub.service.CommentService;
import com.paimonclub.service.InteractionService;
import com.paimonclub.service.PostService;
import com.paimonclub.service.ReportService;
import com.paimonclub.util.QrCodeUtil;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子接口：发布、列表、详情、搜索、评论、点赞、收藏、分享、举报。
 */
@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @Inject PostService postService;
    @Inject CommentService commentService;
    @Inject InteractionService interactionService;
    @Inject ReportService reportService;
    @Inject PostCategoryDao categoryDao;
    @Inject CurrentUser currentUser;

    @GET
    public Result<PageResult<Vos.Post>> page(@QueryParam("categoryId") Long categoryId,
                                             @QueryParam("keyword") String keyword,
                                             @QueryParam("sort") @DefaultValue("NEW") String sort,
                                             @QueryParam("page") @DefaultValue("1") int page,
                                             @QueryParam("size") @DefaultValue("10") int size) {
        return Result.ok(postService.page(categoryId, keyword, sort, page, size));
    }

    @POST
    public Result<Vos.Post> create(@Valid Reqs.Post req) {
        return Result.ok(postService.create(req));
    }

    @GET
    @Path("/hot")
    public Result<List<Vos.Post>> hot(@QueryParam("limit") @DefaultValue("5") int limit) {
        return Result.ok(postService.hot(limit));
    }

    @GET
    @Path("/mine")
    public Result<PageResult<Vos.Post>> mine(@QueryParam("status") String status,
                                             @QueryParam("page") @DefaultValue("1") int page,
                                             @QueryParam("size") @DefaultValue("10") int size) {
        return Result.ok(postService.myPosts(status, page, size));
    }

    @GET
    @Path("/categories")
    public Result<List<Map<String, Object>>> categories() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (PostCategory category : categoryDao.listSorted()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", category.getId());
            map.put("name", category.getName());
            map.put("icon", category.getIcon());
            map.put("description", category.getDescription());
            items.add(map);
        }
        return Result.ok(items);
    }

    @GET
    @Path("/{id}")
    public Result<Vos.Post> detail(@PathParam("id") Long id) {
        return Result.ok(postService.detail(id));
    }

    @PUT
    @Path("/{id}")
    public Result<Vos.Post> update(@PathParam("id") Long id, @Valid Reqs.Post req) {
        return Result.ok(postService.update(id, req));
    }

    @DELETE
    @Path("/{id}")
    public Result<Void> delete(@PathParam("id") Long id) {
        postService.delete(id);
        return Result.ok();
    }

    @GET
    @Path("/{id}/related")
    public Result<List<Vos.Post>> related(@PathParam("id") Long id,
                                          @QueryParam("limit") @DefaultValue("4") int limit) {
        Vos.Post post = postService.detail(id);
        return Result.ok(postService.related(post.categoryId, id, limit));
    }

    @GET
    @Path("/{id}/comments")
    public Result<List<Vos.Comment>> comments(@PathParam("id") Long id) {
        return Result.ok(commentService.tree(id));
    }

    @POST
    @Path("/{id}/comments")
    public Result<Vos.Comment> comment(@PathParam("id") Long id, @Valid Reqs.Comment req) {
        req.postId = id;
        return Result.ok(commentService.add(req));
    }

    @POST
    @Path("/{id}/like")
    public Result<Boolean> like(@PathParam("id") Long id) {
        return Result.ok(interactionService.toggleLike("POST", id));
    }

    @POST
    @Path("/{id}/favorite")
    public Result<Boolean> favorite(@PathParam("id") Long id) {
        return Result.ok(interactionService.toggleFavorite(id));
    }

    @POST
    @Path("/{id}/share")
    public Result<Vos.Share> share(@PathParam("id") Long id, @Context UriInfo uriInfo) {
        // 优先用配置的对外域名，没配才退回请求地址，避免分享出去的是 localhost
        String base = SiteConfig.baseUrl(uriInfo.getBaseUri().toString().replaceAll("/api/?$", ""));
        return Result.ok(interactionService.createShare(id, base));
    }

    @GET
    @Path("/share/{code}")
    public Result<Vos.Post> byShare(@PathParam("code") String code) {
        return Result.ok(interactionService.byShareCode(code));
    }

    @GET
    @Path("/share/{code}/qrcode")
    @Produces("image/png")
    public Response qrcode(@PathParam("code") String code, @Context UriInfo uriInfo) {
        try {
            String base = SiteConfig.baseUrl(uriInfo.getBaseUri().toString().replaceAll("/api/?$", ""));
            String url = base + "/post-detail.html?share=" + code;
            byte[] png = QrCodeUtil.png(url, 320);
            return Response.ok(png).type("image/png").build();
        } catch (Exception ex) {
            throw BusinessException.badRequest("二维码生成失败");
        }
    }

    @POST
    @Path("/{id}/report")
    public Result<Void> report(@PathParam("id") Long id, @Valid Reqs.Report req) {
        req.targetType = "POST";
        req.targetId = id;
        reportService.submit(req);
        return Result.ok();
    }
}
