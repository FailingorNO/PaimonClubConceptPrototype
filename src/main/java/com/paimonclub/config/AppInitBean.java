package com.paimonclub.config;

import com.paimonclub.common.JwtUtil;
import com.paimonclub.service.SeedService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 应用启动钩子：执行安全自检，并按需写入演示数据。
 */
@Singleton
@Startup
public class AppInitBean {

    private static final Logger LOG = Logger.getLogger(AppInitBean.class.getName());

    @Inject
    SeedService seedService;

    @PostConstruct
    public void init() {
        securitySelfCheck();
        try {
            seedService.seed();
            LOG.info("启动初始化完成");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "初始化任务失败（不影响启动，请检查数据库）", ex);
        }
    }

    /**
     * 启动自检：把「上线前必须确认的安全配置」打在启动日志里。
     * 生产环境缺配置时 JwtUtil 已直接阻止启动，这里只负责把当前生效值说清楚。
     */
    private void securitySelfCheck() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ PaimonClub 安全自检 ================\n");
        sb.append("运行环境        : ").append(JwtUtil.env()).append('\n');
        sb.append("JWT 有效期      : ").append(JwtUtil.ttlHours()).append(" 小时\n");
        if (JwtUtil.usingDefaultSecret()) {
            sb.append("JWT 密钥        : 【警告】使用内置默认密钥，仅限本地开发！\n");
            sb.append("                  上线前必须设置 -Dpaimon.jwt.secret 或 PAIMON_JWT_SECRET\n");
        } else {
            sb.append("JWT 密钥        : 已通过外部配置注入\n");
        }
        sb.append("CORS 白名单     : ").append(CorsFilter.allowedOrigins()).append('\n');
        sb.append("====================================================");
        if (JwtUtil.usingDefaultSecret() && JwtUtil.isProduction()) {
            LOG.severe(sb.toString());
        } else {
            LOG.info(sb.toString());
        }
    }
}
