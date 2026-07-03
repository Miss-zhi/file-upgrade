package com.qiwenshare.admin.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解。
 *
 * <p>标注在 Controller 方法上，AOP 切面自动记录操作日志到 operation_log 表。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 模块名（如 user、quota、config）。
     */
    String module();

    /**
     * 操作类型（CREATE、UPDATE、DELETE）。
     */
    String action();

    /**
     * 操作描述。
     */
    String description() default "";
}
