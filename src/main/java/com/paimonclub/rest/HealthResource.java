package com.paimonclub.rest;

import com.paimonclub.common.Result;
import com.paimonclub.service.HealthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * 健康检查接口，用于部署探针与冒烟测试。
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @Inject
    HealthService healthService;

    /** 存活探针：进程在就是 UP */
    @GET
    public Result<Map<String, Object>> health() {
        return Result.ok(healthService.live());
    }

    /** 就绪探针：真实探库，数据库不可用时返回 503 */
    @GET
    @Path("/ready")
    public Response ready() {
        Map<String, Object> data = healthService.ready();
        boolean up = "UP".equals(data.get("status"));
        Result<Map<String, Object>> body = up
                ? Result.ok(data)
                : new Result<>(503, "数据库不可用", data);
        return Response.status(up ? 200 : 503).entity(body).build();
    }
}
