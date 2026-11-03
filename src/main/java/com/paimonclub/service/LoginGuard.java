package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防暴力破解：按「用户名 + 客户端 IP」统计连续失败次数。
 *
 * <p>15 分钟窗口内连续失败 5 次即锁定 15 分钟，成功登录后清零。
 * BCrypt 只能延缓拖库后的离线破解，扛不住在线撞库，所以必须在线限流。</p>
 */
@ApplicationScoped
public class LoginGuard {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final int MAX_ENTRIES = 20_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public String key(String username, String ip) {
        String name = username == null ? "" : username.trim().toLowerCase();
        return name + "@" + (ip == null || ip.isBlank() ? "-" : ip.trim());
    }

    /** 进入登录流程前检查是否处于锁定状态 */
    public void ensureAllowed(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (attempt.lockedUntil > now) {
            long minutes = Math.max(1, (attempt.lockedUntil - now + 59_999) / 60_000);
            throw BusinessException.forbidden("登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
        if (attempt.lockedUntil > 0) {
            attempts.remove(key);
        }
    }

    /** 记录一次失败 */
    public void onFailure(String key) {
        if (attempts.size() > MAX_ENTRIES) {
            attempts.clear();
        }
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, old) -> {
            if (old == null || old.windowStart + WINDOW_MILLIS < now) {
                return new Attempt(1, now, 0);
            }
            int count = old.count + 1;
            long lockedUntil = count >= MAX_FAILURES ? now + WINDOW_MILLIS : 0;
            return new Attempt(count, old.windowStart, lockedUntil);
        });
    }

    /** 登录成功，清空计数 */
    public void onSuccess(String key) {
        attempts.remove(key);
    }

    private static final class Attempt {
        private final int count;
        private final long windowStart;
        private final long lockedUntil;

        private Attempt(int count, long windowStart, long lockedUntil) {
            this.count = count;
            this.windowStart = windowStart;
            this.lockedUntil = lockedUntil;
        }
    }
}
