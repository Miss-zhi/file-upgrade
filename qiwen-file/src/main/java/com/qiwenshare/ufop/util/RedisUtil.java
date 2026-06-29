package com.qiwenshare.ufop.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        if (redisTemplate != null) redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate != null ? redisTemplate.opsForValue().get(key) : null;
    }

    public boolean hasKey(String key) {
        return redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(String key) {
        if (redisTemplate != null) redisTemplate.delete(key);
    }
}
