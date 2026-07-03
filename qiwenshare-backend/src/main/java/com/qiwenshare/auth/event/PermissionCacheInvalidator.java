package com.qiwenshare.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 权限缓存失效监听器。
 *
 * <p>监听 {@link PermissionChangedEvent}，删除受影响用户的 Redis 权限缓存。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheInvalidator {

    private static final String PERM_CACHE_PREFIX = "user:perms:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 处理权限变更事件，删除相关缓存。
     *
     * @param event 权限变更事件
     */
    @EventListener
    public void onPermissionChanged(PermissionChangedEvent event) {
        for (String userId : event.getUserIds()) {
            redisTemplate.delete(PERM_CACHE_PREFIX + userId);
            log.info("权限缓存已清除: userId={}", userId);
        }
    }
}
