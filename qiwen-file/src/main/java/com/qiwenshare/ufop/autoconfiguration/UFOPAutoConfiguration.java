package com.qiwenshare.ufop.autoconfiguration;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * UFOP 自动配置 — 绑定配置属性并初始化 UFOP 工具类
 * <p>
 * 由 {@code @ComponentScan(basePackages = "com.qiwenshare")} 自动发现。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(UFOPConfigProperties.class)
public class UFOPAutoConfiguration {

    public UFOPAutoConfiguration(UFOPConfigProperties properties) {
        // 初始化静态工具类路径
        UFOPUtils.LOCAL_STORAGE_PATH = properties.getLocalStoragePath();
        UFOPUtils.ROOT_PATH = properties.getBucketName();
        log.info("UFOP 自动配置完成: storageType={}, rootPath={}, localStoragePath={}",
                properties.getStorageType(),
                UFOPUtils.ROOT_PATH,
                UFOPUtils.LOCAL_STORAGE_PATH);
    }
}
