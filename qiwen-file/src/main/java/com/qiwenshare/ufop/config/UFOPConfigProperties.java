package com.qiwenshare.ufop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ufop.local")
public class UFOPConfigProperties {
    private String rootPath = "./uploads";
}
