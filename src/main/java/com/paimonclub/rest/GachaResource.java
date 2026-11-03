package com.paimonclub.rest;

import com.paimonclub.common.PageResult;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.GachaAnalysisService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * 抽卡分析接口：导入记录、分析报告、记录分页、清空。
 */
@Path("/gacha")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GachaResource {

    @Inject GachaAnalysisService gachaAnalysisService;

    @POST
    @Path("/import")
    public Result<Integer> importRecords(@Valid Reqs.GachaImport req) {
        return Result.ok(gachaAnalysisService.importRecords(req));
    }

    @GET
    @Path("/analysis")
    public Result<Vos.GachaAnalysis> analysis(@QueryParam("poolName") String poolName) {
        return Result.ok(gachaAnalysisService.analyze(poolName));
    }

    @GET
    @Path("/records")
    public Result<PageResult<Map<String, Object>>> records(@QueryParam("poolName") String poolName,
                                                           @QueryParam("page") @DefaultValue("1") int page,
                                                           @QueryParam("size") @DefaultValue("20") int size) {
        return Result.ok(gachaAnalysisService.page(poolName, page, size));
    }

    @DELETE
    @Path("/records")
    public Result<Void> clear() {
        gachaAnalysisService.clear();
        return Result.ok();
    }
}
