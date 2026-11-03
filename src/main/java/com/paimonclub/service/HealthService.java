package com.paimonclub.service;

import com.paimonclub.dao.UserDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查服务：真正去探一次数据库，而不是永远返回 UP。
 *
 * <p>原来的 /api/health 只回一个常量字符串，数据库挂了、连接池打满了它照样说 UP，
 * 部署脚本和监控都会被骗。这里改成：</p>
 * <ul>
 *   <li>{@code /api/health}：存活探针，只要进程还在就返回 UP，不依赖数据库；</li>
 *   <li>{@code /api/health/ready}：就绪探针，实际执行 {@code select 1}，数据库不可用时返回 DOWN。</li>
 * </ul>
 */
@ApplicationScoped
public class HealthService {

    @PersistenceContext(unitName = "gcPU")
    EntityManager em;

    @Inject
    UserDao userDao;

    @Inject
    LocalCacheService cache;

    /** 存活检查：不碰数据库，避免数据库抖动导致容器被反复重启 */
    public Map<String, Object> live() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("app", "PaimonClub");
        data.put("time", LocalDateTime.now().toString());
        data.put("java", System.getProperty("java.version"));
        return data;
    }

    /** 就绪检查：探数据库 + 缓存指标 */
    @Transactional
    public Map<String, Object> ready() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> db = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        boolean ok;
        try {
            em.createNativeQuery("select 1").getSingleResult();
            db.put("status", "UP");
            db.put("latencyMs", System.currentTimeMillis() - start);
            ok = true;
        } catch (Exception ex) {
            db.put("status", "DOWN");
            db.put("error", ex.getClass().getSimpleName());
            ok = false;
        }
        data.put("status", ok ? "UP" : "DOWN");
        data.put("database", db);
        data.put("cache", cache.stats());
        data.put("time", LocalDateTime.now().toString());
        return data;
    }
}
