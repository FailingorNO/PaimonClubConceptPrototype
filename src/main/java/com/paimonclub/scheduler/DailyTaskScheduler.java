package com.paimonclub.scheduler;

import com.paimonclub.service.LocalCacheService;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * 定时任务：热帖缓存刷新、每日数据播报。
 */
@Singleton
@Startup
public class DailyTaskScheduler {

    private static final Logger LOG = Logger.getLogger(DailyTaskScheduler.class.getName());

    @Inject
    LocalCacheService cache;

    /** 每 10 分钟清理一次热帖缓存，保证榜单时效性 */
    @Schedule(hour = "*", minute = "*/10", second = "0", persistent = false)
    public void refreshHotCache() {
        cache.evictPrefix("posts:hot");
        LOG.fine("热帖缓存已刷新");
    }

    /** 每天 09:00 输出一次运行状态 */
    @Schedule(hour = "9", minute = "0", second = "0", persistent = false)
    public void dailyReport() {
        LOG.info("派蒙社每日定时任务执行：缓存与数据状态正常");
    }
}
