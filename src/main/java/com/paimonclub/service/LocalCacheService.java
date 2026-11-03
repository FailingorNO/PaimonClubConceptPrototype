package com.paimonclub.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * 进程内 TTL + LRU 缓存。
 *
 * <p>命名说明：本项目并未引入 Redis，之前叫 {@code RedisCacheService} 会让人误以为连的是 Redis，
 * 实际是单机进程内 Map。这里改名为 {@code LocalCacheService}，行为如下：</p>
 *
 * <ul>
 *   <li>TTL：每条记录到期即失效；</li>
 *   <li>容量上限：默认 2000 条，可通过 {@code -Dpaimon.cache.max-entries=5000} 调整，
 *       超出后按 LRU 淘汰最久未访问的记录，避免高基数 key（比如每个用户一个缓存）
 *       把堆内存吃满；</li>
 *   <li>后台每 60 秒清理一次过期记录，防止冷数据长期占位；</li>
 *   <li>命中/未命中计数，便于排查缓存是否真的起作用。</li>
 * </ul>
 *
 * <p>如果后续要换成真正的 Redis，只需让本类改为委托 Redis 客户端，
 * 业务层调用的方法签名保持不变即可。</p>
 */
@ApplicationScoped
public class LocalCacheService {

    private static final Logger LOG = Logger.getLogger(LocalCacheService.class.getName());

    private static final int DEFAULT_MAX_ENTRIES = 2000;

    private final int maxEntries;

    /** accessOrder = true 的 LinkedHashMap 天然就是 LRU */
    private final Map<String, LocalCacheService.Entry> store;

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder evictions = new LongAdder();

    private ScheduledExecutorService cleaner;

    public LocalCacheService() {
        this.maxEntries = resolveMaxEntries();
        this.store = new LinkedHashMap<String, LocalCacheService.Entry>(256, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, LocalCacheService.Entry> eldest) {
                if (size() > maxEntries) {
                    evictions.increment();
                    return true;
                }
                return false;
            }
        };
    }

    private static int resolveMaxEntries() {
        String raw = System.getProperty("paimon.cache.max-entries");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("PAIMON_CACHE_MAX_ENTRIES");
        }
        if (raw != null && !raw.isBlank()) {
            try {
                int value = Integer.parseInt(raw.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // 配置写错就退回默认值，不要因为一个环境变量让应用起不来
            }
        }
        return DEFAULT_MAX_ENTRIES;
    }

    @PostConstruct
    void init() {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "paimon-cache-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleWithFixedDelay(this::purgeExpired, 60, 60, TimeUnit.SECONDS);
        LOG.info("本地缓存初始化完成：maxEntries=" + maxEntries);
    }

    @PreDestroy
    void shutdown() {
        if (cleaner != null) {
            cleaner.shutdownNow();
        }
    }

    public synchronized void put(String key, Object value, long ttlSeconds) {
        if (key == null || ttlSeconds <= 0) {
            return;
        }
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlSeconds * 1000L));
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T get(String key, Class<T> type) {
        Entry entry = store.get(key);
        if (entry == null) {
            misses.increment();
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            store.remove(key);
            misses.increment();
            return null;
        }
        Object value = entry.value;
        if (!type.isInstance(value)) {
            misses.increment();
            return null;
        }
        hits.increment();
        return (T) value;
    }

    public synchronized void evict(String key) {
        store.remove(key);
    }

    /** 按前缀批量失效 */
    public synchronized void evictPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
        store.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public synchronized void clear() {
        store.clear();
    }

    public synchronized int size() {
        return store.size();
    }

    /** 命中率等运行指标，供健康检查或后台排查使用 */
    public String stats() {
        long hit = hits.sum();
        long miss = misses.sum();
        long total = hit + miss;
        double rate = total == 0 ? 0 : (hit * 100.0 / total);
        return String.format("size=%d/%d, hit=%d, miss=%d, hitRate=%.1f%%, evicted=%d",
                size(), maxEntries, hit, miss, rate, evictions.sum());
    }

    private synchronized void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    private static final class Entry {
        private final Object value;
        private final long expireAt;

        private Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
