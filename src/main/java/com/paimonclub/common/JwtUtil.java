package com.paimonclub.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：HS256 自签令牌。
 *
 * <p>密钥解析优先级：系统属性 {@code paimon.jwt.secret} &gt; 环境变量 {@code PAIMON_JWT_SECRET} &gt; 内置默认值。
 * 内置默认值只允许在开发环境使用：生产环境（{@code paimon.env=prod}）未配置密钥时，
 * 本类在类初始化阶段直接抛出 {@link IllegalStateException}，让应用启动失败，而不是带着公开密钥上线。</p>
 *
 * <p>有效期可通过 {@code paimon.jwt.ttl-hours} 配置，默认 168 小时（7 天），上限同样为 168 小时。</p>
 */
public final class JwtUtil {

    /** 内置默认密钥，仅开发环境使用 */
    private static final String DEFAULT_SECRET =
            "paimon-club-jwt-secret-key-2026-change-me-in-production-environment";
    private static final long MAX_TTL_HOURS = 7L * 24;

    private static final String SECRET = resolveSecret();
    private static final boolean USING_DEFAULT = DEFAULT_SECRET.equals(SECRET);
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long TTL_MILLIS = resolveTtlHours() * 60 * 60 * 1000;

    private JwtUtil() {
    }

    /** 当前运行环境：dev / test / prod，默认 dev */
    public static String env() {
        String env = System.getProperty("paimon.env");
        if (env == null || env.isBlank()) {
            env = System.getenv("PAIMON_ENV");
        }
        if (env == null || env.isBlank()) {
            return "dev";
        }
        return env.trim().toLowerCase();
    }

    public static boolean isProduction() {
        return "prod".equals(env()) || "production".equals(env());
    }

    /** 是否仍在用内置默认密钥（仅开发环境可能为 true） */
    public static boolean usingDefaultSecret() {
        return USING_DEFAULT;
    }

    public static long ttlHours() {
        return TTL_MILLIS / (60 * 60 * 1000);
    }

    private static String resolveSecret() {
        String secret = System.getProperty("paimon.jwt.secret");
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("PAIMON_JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            if (isProduction()) {
                throw new IllegalStateException(
                        "生产环境未配置 JWT 密钥。请设置系统属性 -Dpaimon.jwt.secret 或环境变量 PAIMON_JWT_SECRET"
                                + "（至少 32 字节），禁止使用内置默认密钥上线。");
            }
            return DEFAULT_SECRET;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足：当前 " + bytes.length + " 字节，HS256 要求至少 32 字节。");
        }
        return secret;
    }

    private static long resolveTtlHours() {
        String raw = System.getProperty("paimon.jwt.ttl-hours");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("PAIMON_JWT_TTL_HOURS");
        }
        if (raw == null || raw.isBlank()) {
            return MAX_TTL_HOURS;
        }
        try {
            long hours = Long.parseLong(raw.trim());
            if (hours <= 0) {
                return MAX_TTL_HOURS;
            }
            return Math.min(hours, MAX_TTL_HOURS);
        } catch (NumberFormatException ex) {
            return MAX_TTL_HOURS;
        }
    }

    /** 签发令牌 */
    public static String generate(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_MILLIS))
                .signWith(KEY)
                .compact();
    }

    /** 校验并解析令牌，非法或过期抛 JwtException */
    public static Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }

    public static Long userId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }
}
