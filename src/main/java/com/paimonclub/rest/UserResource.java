package com.paimonclub.rest;

import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.NotificationService;
import com.paimonclub.service.PointsService;
import com.paimonclub.service.UserService;
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
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 用户接口：资料、签到、积分、通知、用户搜索。
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService userService;
    @Inject PointsService pointsService;
    @Inject NotificationService notificationService;
    @Inject CurrentUser currentUser;

    @GET
    @Path("/me")
    public Result<Vos.User> me() {
        return Result.ok(userService.me());
    }

    @PUT
    @Path("/me")
    public Result<Vos.User> update(@Valid Reqs.Profile req) {
        return Result.ok(userService.updateProfile(req));
    }

    @PUT
    @Path("/me/password")
    public Result<Void> changePassword(@Valid Reqs.Password req) {
        userService.changePassword(req);
        return Result.ok();
    }

    @POST
    @Path("/sign-in")
    public Result<Vos.SignIn> signIn() {
        return Result.ok(userService.signIn());
    }

    @GET
    @Path("/points/records")
    public Result<PageResult<Vos.PointsRecord>> points(@QueryParam("page") @DefaultValue("1") int page,
                                                       @QueryParam("size") @DefaultValue("10") int size) {
        Long userId = currentUser.requireLogin();
        return Result.ok(pointsService.page(userId, page, size));
    }

    @GET
    @Path("/points/ranking")
    public Result<List<Vos.User>> ranking(@QueryParam("limit") @DefaultValue("10") int limit) {
        return Result.ok(userService.pointsRanking(limit));
    }

    @GET
    @Path("/notifications")
    public Result<PageResult<Vos.Notification>> notifications(@QueryParam("page") @DefaultValue("1") int page,
                                                             @QueryParam("size") @DefaultValue("10") int size) {
        Long userId = currentUser.requireLogin();
        return Result.ok(notificationService.page(userId, page, size));
    }

    @POST
    @Path("/notifications/read")
    public Result<Integer> readAll() {
        Long userId = currentUser.requireLogin();
        return Result.ok(notificationService.markAllRead(userId));
    }

    @GET
    @Path("/notifications/unread")
    public Result<Long> unread() {
        return Result.ok(notificationService.unread(currentUser.getUserId()));
    }

    @GET
    @Path("/search")
    public Result<PageResult<Vos.User>> search(@QueryParam("keyword") String keyword,
                                               @QueryParam("page") @DefaultValue("1") int page,
                                               @QueryParam("size") @DefaultValue("10") int size) {
        return Result.ok(userService.search(keyword, page, size));
    }

    @GET
    @Path("/{id}")
    public Result<Vos.User> profile(@PathParam("id") Long id) {
        return Result.ok(userService.profile(id));
    }
}
