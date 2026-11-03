package com.paimonclub.service;

import com.paimonclub.dao.UserDao;
import com.paimonclub.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鉴权上下文加载：把「令牌里的用户快照」换成「数据库里的最新用户状态」。
 *
 * <p>无状态 JWT 无法主动失效，所以每个请求都要确认账号仍然可用。为了不让每个请求都打库，
 * 这里做了 5 秒的短缓存：管理员封禁 / 降权最多 5 秒内生效，同时把鉴权查询量压到可接受范围。</p>
 */
@ApplicationScoped
public class AuthCacheService {

    /** 缓存有效期：5 秒，兼顾封禁实时性与数据库压力 */
    private static final long TTL_MILLIS = 5_000L;
    private static final int MAX_ENTRIES = 4096;

    @Inject
    UserDao userDao;

    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    @Transactional
    public User load(Long userId) {
        if (userId == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        Entry cached = cache.get(userId);
        if (cached != null && cached.expireAt > now) {
            return cached.user;
        }
        User user = userDao.find(userId);
        if (cache.size() > MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(userId, new Entry(user, now + TTL_MILLIS));
        return user;
    }

    /** 管理员改了用户状态 / 角色后调用，立即失效 */
    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    public void evictAll() {
        cache.clear();
    }

    private static final class Entry {
        private final User user;
        private final long expireAt;

        private Entry(User user, long expireAt) {
            this.user = user;
            this.expireAt = expireAt;
        }
    }
}
