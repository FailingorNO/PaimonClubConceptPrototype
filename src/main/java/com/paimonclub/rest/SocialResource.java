package com.paimonclub.rest;

import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.SocialService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 社交接口：关注、好友、私信。
 */
@Path("/social")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SocialResource {

    @Inject SocialService socialService;
    @Inject CurrentUser currentUser;

    @POST
    @Path("/follow/{userId}")
    public Result<Boolean> follow(@PathParam("userId") Long userId) {
        return Result.ok(socialService.toggleFollow(userId));
    }

    @GET
    @Path("/followers/{userId}")
    public Result<List<Vos.User>> followers(@PathParam("userId") Long userId) {
        return Result.ok(socialService.followers(userId));
    }

    @GET
    @Path("/followees/{userId}")
    public Result<List<Vos.User>> followees(@PathParam("userId") Long userId) {
        return Result.ok(socialService.followees(userId));
    }

    @POST
    @Path("/friends/action")
    public Result<String> friendAction(@Valid Reqs.FriendAction req) {
        return Result.ok(socialService.friendAction(req));
    }

    @GET
    @Path("/friends")
    public Result<List<Vos.RelationItem>> friends() {
        return Result.ok(socialService.friends());
    }

    @GET
    @Path("/friends/requests")
    public Result<List<Vos.RelationItem>> friendRequests() {
        return Result.ok(socialService.friendRequests());
    }

    @GET
    @Path("/friends/blocked")
    public Result<List<Vos.RelationItem>> blocked() {
        return Result.ok(socialService.blocked());
    }

    @GET
    @Path("/messages/inbox")
    public Result<List<Vos.Message>> inbox() {
        return Result.ok(socialService.inbox());
    }

    @GET
    @Path("/messages/{userId}")
    public Result<List<Vos.Message>> conversation(@PathParam("userId") Long userId) {
        return Result.ok(socialService.conversation(userId));
    }

    @POST
    @Path("/messages")
    public Result<Vos.Message> send(@Valid Reqs.Message req) {
        return Result.ok(socialService.sendMessage(req));
    }
}
