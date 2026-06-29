package com.qiwenshare.ufop.config;

import com.qiwenshare.ufop.domain.AliyunOSS;
import lombok.Data;

/**
 * 阿里云 OSS 配置
 */
@Data
public class AliyunConfig {

    private AliyunOSS oss = new AliyunOSS();
}
