package com.paimonclub.rest;

import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.service.DecorationService;
import com.paimonclub.service.MallService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 积分商城与装扮接口：商品浏览、积分兑换、订单、装扮购买与佩戴。
 */
@Path("/mall")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MallResource {

    @Inject MallService mallService;
    @Inject DecorationService decorationService;
    @Inject CurrentUser currentUser;

    @GET
    @Path("/products")
    public Result<PageResult<Vos.Product>> products(@QueryParam("keyword") String keyword,
                                                    @QueryParam("type") String type,
                                                    @QueryParam("page") @DefaultValue("1") int page,
                                                    @QueryParam("size") @DefaultValue("12") int size) {
        return Result.ok(mallService.page(keyword, type, page, size));
    }

    @GET
    @Path("/products/on-sale")
    public Result<List<Vos.Product>> onSale() {
        return Result.ok(mallService.onSale());
    }

    @POST
    @Path("/exchange")
    public Result<Vos.Order> exchange(@Valid Reqs.Exchange req) {
        return Result.ok(mallService.exchange(req));
    }

    @GET
    @Path("/orders/mine")
    public Result<PageResult<Vos.Order>> myOrders(@QueryParam("page") @DefaultValue("1") int page,
                                                  @QueryParam("size") @DefaultValue("10") int size) {
        return Result.ok(mallService.myOrders(page, size));
    }

    @POST
    @Path("/orders/{id}/ship")
    public Result<Vos.Order> ship(@PathParam("id") Long id, @Valid Reqs.Ship req) {
        return Result.ok(mallService.ship(id, req.logisticsNo));
    }

    @POST
    @Path("/orders/{id}/finish")
    public Result<Vos.Order> finish(@PathParam("id") Long id) {
        return Result.ok(mallService.finish(id));
    }

    @POST
    @Path("/orders/{id}/cancel")
    public Result<Vos.Order> cancel(@PathParam("id") Long id) {
        return Result.ok(mallService.cancel(id));
    }

    @GET
    @Path("/orders")
    public Result<PageResult<Vos.Order>> allOrders(@QueryParam("page") @DefaultValue("1") int page,
                                                   @QueryParam("size") @DefaultValue("10") int size) {
        currentUser.requireStaff();
        return Result.ok(mallService.allOrders(page, size));
    }

    @POST
    @Path("/products")
    public Result<Vos.Product> saveProduct(@Valid Reqs.Product req) {
        currentUser.requireStaff();
        return Result.ok(mallService.save(req));
    }

    @DELETE
    @Path("/products/{id}")
    public Result<Void> deleteProduct(@PathParam("id") Long id) {
        currentUser.requireStaff();
        mallService.delete(id);
        return Result.ok();
    }

    @GET
    @Path("/decorations")
    public Result<List<Vos.Decoration>> decorations() {
        return Result.ok(decorationService.store());
    }

    @GET
    @Path("/decorations/mine")
    public Result<List<Vos.Decoration>> myDecorations() {
        return Result.ok(decorationService.mine());
    }

    @POST
    @Path("/decorations/{id}/buy")
    public Result<Vos.Decoration> buy(@PathParam("id") Long id) {
        return Result.ok(decorationService.buy(id));
    }

    @POST
    @Path("/decorations/{id}/wear")
    public Result<Void> wear(@PathParam("id") Long id,
                             @QueryParam("wearing") @DefaultValue("true") boolean wearing) {
        decorationService.wear(id, wearing);
        return Result.ok();
    }

    @DELETE
    @Path("/decorations/{id}/wear")
    public Result<Void> unwear(@PathParam("id") Long id) {
        decorationService.wear(id, false);
        return Result.ok();
    }
}
