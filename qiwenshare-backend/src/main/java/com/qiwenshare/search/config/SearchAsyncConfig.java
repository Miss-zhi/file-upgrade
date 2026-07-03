package com.qiwenshare.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 搜索模块异步线程池配置。
 *
 * <p>为索引同步操作提供独立的 {@code searchIndexExecutor}，
 * 与 {@code fileTaskExecutor} 分离，避免索引 IO 影响文件任务。</p>
 */
@Configuration
public class SearchAsyncConfig {

    /**
     * 搜索索引异步执行器。
     *
     * @param core  核心线程数
     * @param max   最大线程数
     * @param queue 队列容量
     * @return TaskExecutor
     */
    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor(
            @Value("${async.search.core-pool-size:2}") int core,
            @Value("${async.search.max-pool-size:4}") int max,
            @Value("${async.search.queue-capacity:200}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("search-index-");
        executor.initialize();
        return executor;
    }
}
