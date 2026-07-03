package com.qiwenshare.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置类。
 *
 * <p>配置 {@link StringRedisTemplate} 用于 token 黑名单、登录失败计数、
 * refresh token 注册、权限缓存等场景。Key 和 Value 均使用 String 序列化。</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 StringRedisTemplate Bean。
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
