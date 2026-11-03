package com.paimonclub.common;

/**
 * 站点外部访问地址解析。
 *
 * <p>分享链接、二维码里不能直接写死 {@code http://localhost:8080}：
 * 部署到服务器或者被 Nginx 反代之后，用户拿到的分享链接会指向他自己电脑的 localhost，根本打不开。</p>
 *
 * <p>取值顺序：</p>
 * <ol>
 *   <li>系统属性 {@code -Dpaimon.site.base-url=https://club.example.com}</li>
 *   <li>环境变量 {@code PAIMON_SITE_BASE_URL}</li>
 *   <li>都未配置时，退回请求本身的地址（适合本机开发）</li>
 * </ol>
 */
public final class SiteConfig {

    private SiteConfig() {
    }

    /**
     * @param fallback 请求上下文推导出来的地址，配置缺失时使用
     * @return 不带结尾斜杠的站点根地址
     */
    public static String baseUrl(String fallback) {
        String configured = firstNonBlank(
                System.getProperty("paimon.site.base-url"),
                System.getenv("PAIMON_SITE_BASE_URL"));
        String base = configured == null ? fallback : configured;
        if (base == null || base.isBlank()) {
            return "";
        }
        return base.trim().replaceAll("/+$", "");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
