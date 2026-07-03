package com.qiwenshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 奇文网盘应用主入口。
 *
 * <p>启用所有自动配置，扫描 {@code com.qiwenshare} 包下的组件。
 * {@code @ConfigurationPropertiesScan} 确保 {@code @ConfigurationProperties} 注解的
 * record 类（如 JwtProperties、AuthProperties）被正确绑定。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class QiwenshareApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiwenshareApplication.class, args);
    }
}
