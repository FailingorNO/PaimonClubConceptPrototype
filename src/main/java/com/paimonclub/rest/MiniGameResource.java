package com.paimonclub.rest;

import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.MiniGameService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * 小游戏接口：列表、分数上传、排行榜。
 */
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MiniGameResource {

    @Inject MiniGameService miniGameService;

    @GET
    public Result<List<Vos.MiniGame>> list() {
        return Result.ok(miniGameService.list());
    }

    @POST
    @Path("/score")
    public Result<Map<String, Object>> submit(@Valid Reqs.Score req) {
        return Result.ok(miniGameService.submitScore(req));
    }

    @GET
    @Path("/{id}/leaderboard")
    public Result<List<Vos.Score>> leaderboard(@PathParam("id") Long id,
                                               @QueryParam("limit") @DefaultValue("10") int limit) {
        return Result.ok(miniGameService.leaderboard(id, limit));
    }
}
