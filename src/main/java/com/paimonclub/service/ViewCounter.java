package com.paimonclub.service;

import com.paimonclub.dao.PostDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 浏览量计数器：内存累加 + 定时批量落库。
 *
 * <p>原来每打开一次帖子详情就 {@code update t_post set view_count = view_count + 1}，
 * 热门帖子会把同一行的行锁打满，详情页读请求互相排队。</p>
 *
 * <p>现在详情页只在内存里 +1，后台每 20 秒把累计值合并成一次 UPDATE 批量写入：
 * 20 秒内某帖被看 1000 次，也只会产生 1 条 SQL。进程正常关闭时会 flush 一次，尽量不丢数据。</p>
 */
@ApplicationScoped
public class ViewCounter {

    private static final Logger LOG = Logger.getLogger(ViewCounter.class.getName());

    private final Map<Long, AtomicInteger> pending = new ConcurrentHashMap<>();

    @Inject
    PostDao postDao;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "paimon-view-flusher");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::flushQuietly, 20, 20, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        flushQuietly();
    }

    /** 记录一次浏览（内存操作，不进数据库） */
    public void hit(Long postId) {
        if (postId == null) {
            return;
        }
        pending.computeIfAbsent(postId, k -> new AtomicInteger()).incrementAndGet();
    }

    /** 还有多少浏览没落库，详情页展示时补上，避免用户看到「刚看过的帖子浏览量没变」 */
    public int pendingOf(Long postId) {
        AtomicInteger counter = postId == null ? null : pending.get(postId);
        return counter == null ? 0 : counter.get();
    }

    private void flushQuietly() {
        try {
            // 通过 CDI 代理调用，@Transactional 拦截器才会生效
            CDI.current().select(ViewCounter.class).get().flush();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "浏览量批量落库失败，将在下一轮重试", ex);
        }
    }

    @Transactional
    public void flush() {
        if (pending.isEmpty()) {
            return;
        }
        Map<Long, Integer> batch = new HashMap<>();
        for (Long postId : new ArrayList<>(pending.keySet())) {
            AtomicInteger counter = pending.remove(postId);
            if (counter != null && counter.get() > 0) {
                batch.put(postId, counter.get());
            }
        }
        for (Map.Entry<Long, Integer> entry : batch.entrySet()) {
            postDao.incrementViewBy(entry.getKey(), entry.getValue());
        }
    }
}
