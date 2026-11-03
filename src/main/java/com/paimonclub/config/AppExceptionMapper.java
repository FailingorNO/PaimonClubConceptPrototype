package com.paimonclub.config;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 全局异常 → 统一响应 {code, msg}。
 */
@Provider
public class AppExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(AppExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof BusinessException be) {
            return build(statusOf(be.getCode()), Result.fail(be.getCode(), be.getMessage()));
        }
        if (ex instanceof ConstraintViolationException cve) {
            String msg = cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("；"));
            return build(Response.Status.BAD_REQUEST, Result.fail(Constants.CODE_BAD_REQUEST, msg));
        }
        if (ex instanceof WebApplicationException wae) {
            int status = wae.getResponse() == null ? 500 : wae.getResponse().getStatus();
            String msg = status == 404 ? "接口不存在" : (wae.getMessage() == null ? "请求失败" : wae.getMessage());
            return build(Response.Status.fromStatusCode(status) == null ? Response.Status.BAD_REQUEST
                    : Response.Status.fromStatusCode(status), Result.fail(status, msg));
        }
        LOG.log(Level.SEVERE, "未处理异常", ex);
        return build(Response.Status.INTERNAL_SERVER_ERROR,
                Result.fail(Constants.CODE_SERVER_ERROR, "服务器内部错误：" + safeMessage(ex)));
    }

    private String safeMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return ex.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    private Response.Status statusOf(int code) {
        return switch (code) {
            case 401 -> Response.Status.UNAUTHORIZED;
            case 403 -> Response.Status.FORBIDDEN;
            case 404 -> Response.Status.NOT_FOUND;
            case 500 -> Response.Status.INTERNAL_SERVER_ERROR;
            default -> Response.Status.BAD_REQUEST;
        };
    }

    private Response build(Response.Status status, Result<?> body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
