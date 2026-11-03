package com.paimonclub.rest;

import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.AuthService;
import com.paimonclub.service.UserService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * 认证接口：注册、登录、当前用户。
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;
    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public Result<Vos.LoginResult> register(@Valid Reqs.Register req) {
        return Result.ok(authService.register(req));
    }

    @POST
    @Path("/login")
    public Result<Vos.LoginResult> login(@Valid Reqs.Login req, @Context HttpServletRequest request) {
        return Result.ok(authService.login(req, clientIp(request)));
    }

    @GET
    @Path("/me")
    public Result<Vos.User> me() {
        return Result.ok(userService.me());
    }

    /** 取真实客户端 IP：优先 X-Forwarded-For 第一段，其次 remoteAddr */
    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
