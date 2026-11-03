package com.paimonclub.rest;

import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.GameAccount;
import com.paimonclub.entity.GameCharacter;
import com.paimonclub.service.GameAccountService;
import com.paimonclub.util.DateUtil;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏数据接口：绑定 UID、查询角色数据、录入/删除角色、解绑。
 */
@Path("/game")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameDataResource {

    @Inject GameAccountService gameAccountService;
    @Inject CurrentUser currentUser;

    @POST
    @Path("/account")
    public Result<Map<String, Object>> bind(@Valid Reqs.BindGame req) {
        GameAccount account = gameAccountService.bind(req);
        return Result.ok(toMap(account));
    }

    @GET
    @Path("/account")
    public Result<Map<String, Object>> account() {
        GameAccount account = gameAccountService.myAccount();
        if (account == null) {
            return Result.ok(null);
        }
        return Result.ok(toMap(account));
    }

    @DELETE
    @Path("/account")
    public Result<Void> unbind() {
        gameAccountService.unbind();
        return Result.ok();
    }

    @GET
    @Path("/data")
    public Result<Vos.GameData> data(@QueryParam("gameUid") String gameUid) {
        return Result.ok(gameAccountService.query(gameUid));
    }

    @GET
    @Path("/characters")
    public Result<List<GameCharacter>> characters() {
        return Result.ok(gameAccountService.listCharacters());
    }

    @POST
    @Path("/characters")
    public Result<GameCharacter> addCharacter(@Valid Reqs.GameCharacter req) {
        return Result.ok(gameAccountService.addCharacter(req));
    }

    @DELETE
    @Path("/characters/{id}")
    public Result<Void> removeCharacter(@PathParam("id") Long id) {
        gameAccountService.removeCharacter(id);
        return Result.ok();
    }

    private Map<String, Object> toMap(GameAccount account) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", account.getId());
        map.put("gameUid", account.getGameUid());
        map.put("server", account.getServer());
        map.put("gameName", account.getGameName());
        map.put("nickname", account.getNickname());
        map.put("adventureRank", account.getAdventureRank());
        map.put("worldLevel", account.getWorldLevel());
        map.put("bindTime", DateUtil.format(account.getBindTime()));
        return map;
    }
}
