package com.paimonclub.rest;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Result;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.util.FileStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * 文件接口：图片上传与读取。
 */
@Path("/files")
public class FileResource {

    @Inject FileStore fileStore;

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result<Map<String, String>> upload(Reqs.Upload req) {
        String url = fileStore.saveDataUrl(req.dataUrl);
        return Result.ok(Map.of("url", url));
    }

    @GET
    @Path("/{name}")
    public Response get(@PathParam("name") String name) {
        java.nio.file.Path path;
        try {
            path = fileStore.resolve(name);
        } catch (BusinessException ex) {
            throw ex;
        }
        if (!Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String type = "application/octet-stream";
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) {
            type = "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            type = "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            type = "image/gif";
        } else if (lower.endsWith(".webp")) {
            type = "image/webp";
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return Response.ok(bytes).type(type)
                    .header("Cache-Control", "public, max-age=86400").build();
        } catch (IOException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
