package com.paimonclub.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 访问日志 + 请求追踪 ID。
 *
 * <p>原来出了 500 只能靠翻服务器日志猜是哪个请求。这里给每个请求生成一个 traceId，
 * 同时写进响应头 {@code X-Trace-Id} 和访问日志，前端报错时把响应头贴过来就能直接定位。</p>
 *
 * <p>日志分级：5xx 用 SEVERE、4xx 用 WARNING、2xx/3xx 用 INFO；健康检查不记录，避免刷屏。</p>
 */
@Provider
@Priority(Priorities.USER)
public class TraceFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(TraceFilter.class.getName());

    public static final String HEADER = "X-Trace-Id";
    private static final String PROP_ID = "paimon.traceId";
    private static final String PROP_START = "paimon.traceStart";

    @Override
    public void filter(ContainerRequestContext request) {
        String traceId = request.getHeaderString(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        request.setProperty(PROP_ID, traceId);
        request.setProperty(PROP_START, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        Object id = request.getProperty(PROP_ID);
        if (id != null) {
            response.getHeaders().add(HEADER, id);
        }
        String path = request.getUriInfo().getPath();
        if (path == null || path.startsWith("health")) {
            return;
        }
        Object start = request.getProperty(PROP_START);
        long cost = start instanceof Long ? System.currentTimeMillis() - (Long) start : -1L;
        int status = response.getStatus();
        String line = "[" + id + "] " + request.getMethod() + " /api/" + path
                + " -> " + status + " (" + cost + "ms)";
        if (status >= 500) {
            LOG.severe(line);
        } else if (status >= 400) {
            LOG.warning(line);
        } else {
            LOG.info(line);
        }
    }
}
