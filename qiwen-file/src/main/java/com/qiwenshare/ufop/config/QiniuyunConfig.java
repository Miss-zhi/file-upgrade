package com.qiwenshare.ufop.config;

import com.qiwenshare.ufop.domain.QiniuyunKodo;
import lombok.Data;

/**
 * 七牛云 Kodo 配置
 */
@Data
public class QiniuyunConfig {

    private QiniuyunKodo kodo = new QiniuyunKodo();
}
