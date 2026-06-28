package com.qiwenshare.file.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP 操作日志切面
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    @Around("@annotation(com.qiwenshare.file.aop.MyLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MyLog myLog = method.getAnnotation(MyLog.class);

        String module = myLog.module();
        String desc = myLog.value();
        Object[] args = joinPoint.getArgs();

        log.info("[{}] 操作开始: {} — 参数: {}", module, desc, Arrays.toString(args));
        long start = System.currentTimeMillis();

        Object result;
        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] 操作完成: {} — 耗时: {}ms", module, desc, elapsed);
        } catch (Exception e) {
            log.error("[{}] 操作异常: {} — {}", module, desc, e.getMessage());
            throw e;
        }

        return result;
    }
}
