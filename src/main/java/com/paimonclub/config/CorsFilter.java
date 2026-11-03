package com.paimonclub.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 跨域过滤器（开发期前端可能独立运行）。
 *
 * <p>允许的来源通过 {@code -Dpaimon.cors.origins} 或环境变量 {@code PAIMON_CORS_ORIGINS}
 * 配置（逗号分隔），默认只放行本机 8080。不再无条件回 {@code *}：配合 localStorage 存令牌时，
 * 通配来源等于把令牌交给任意钓鱼页面。</p>
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(CorsFilter.class.getName());

    private static final Set<String> ALLOWED = resolveAllowed();

    private static Set<String> resolveAllowed() {
        String raw = System.getProperty("paimon.cors.origins");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("PAIMON_CORS_ORIGINS");
        }
        if (raw == null || raw.isBlank()) {
            raw = "http://localhost:8080,http://127.0.0.1:8080";
        }
        Set<String> set = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(set::add);
        LOG.info("CORS 白名单: " + set);
        return set;
    }

    public static Set<String> allowedOrigins() {
        return ALLOWED;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String origin = requestContext.getHeaderString("Origin");
            Response.ResponseBuilder builder = Response.ok();
            if (origin != null && ALLOWED.contains(origin)) {
                builder.header("Access-Control-Allow-Origin", origin)
                        .header("Vary", "Origin")
                        .header("Access-Control-Allow-Credentials", "true");
            }
            requestContext.abortWith(builder.build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.putSingle("Vary", "Origin");
        if (origin == null || !ALLOWED.contains(origin)) {
            return;
        }
        headers.putSingle("Access-Control-Allow-Origin", origin);
        headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        headers.putSingle("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        headers.putSingle("Access-Control-Allow-Credentials", "true");
        headers.putSingle("Access-Control-Max-Age", "86400");
    }
}
