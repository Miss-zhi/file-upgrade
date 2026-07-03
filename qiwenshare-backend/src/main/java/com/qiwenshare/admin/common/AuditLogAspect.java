package com.qiwenshare.admin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.admin.entity.OperationLog;
import com.qiwenshare.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志 AOP 切面。
 *
 * <p>拦截所有标注 {@link AuditLog} 的方法，异步记录操作日志到 operation_log 表。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private static final String[] SENSITIVE_KEYS = {"password", "newPassword", "oldPassword", "confirmPassword"};

    private final OperationLogAsyncWriter asyncWriter;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知，记录审计日志。
     *
     * @param joinPoint 连接点
     * @param auditLog  注解实例
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog opLog = buildBaseLog(auditLog, joinPoint);

        try {
            Object result = joinPoint.proceed();
            opLog.setResponseCode(200);
            return result;
        } catch (Throwable ex) {
            opLog.setResponseCode(resolveHttpStatus(ex));
            opLog.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            opLog.setExecutionTime(System.currentTimeMillis() - startTime);
            asyncWriter.save(opLog);
        }
    }

    private OperationLog buildBaseLog(AuditLog auditLog, ProceedingJoinPoint joinPoint) {
        OperationLog opLog = new OperationLog();
        opLog.setModule(auditLog.module());
        opLog.setAction(auditLog.action());
        opLog.setDescription(auditLog.description());

        // 获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            // userDetails.getUsername() 存储的是 Snowflake 业务 ID（非真实用户名）
            String userId = userDetails.getUsername();
            opLog.setUserId(userId);
            // 从 DB 查找真实用户名
            userRepository.findByUserId(userId)
                    .ifPresent(user -> opLog.setUsername(user.getUsername()));
        }

        // 获取请求信息
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            opLog.setRequestMethod(request.getMethod());
            opLog.setRequestUri(request.getRequestURI());
            opLog.setIpAddress(getClientIp(request));
            String ua = request.getHeader("User-Agent");
            opLog.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);

            // 序列化请求参数（脱敏）
            Object[] args = joinPoint.getArgs();
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            opLog.setRequestParams(sanitizeAndSerialize(paramNames, args));
        }

        return opLog;
    }

    private String sanitizeAndSerialize(String[] paramNames, Object[] args) {
        try {
            Map<String, Object> paramMap = new LinkedHashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                Object value = args[i];
                // 跳过 HttpServletRequest/Response 等不可序列化对象
                if (value instanceof jakarta.servlet.http.HttpServletRequest ||
                    value instanceof jakarta.servlet.http.HttpServletResponse ||
                    value instanceof org.springframework.security.core.Authentication) {
                    paramMap.put(paramNames[i], "[SKIPPED]");
                } else {
                    paramMap.put(paramNames[i], value);
                }
            }
            String json = objectMapper.writeValueAsString(paramMap);
            // 脱敏处理
            for (String key : SENSITIVE_KEYS) {
                json = json.replaceAll("\"" + key + "\"\\s*:\\s*\"[^\"]*\"", "\"" + key + "\":\"******\"");
            }
            return json;
        } catch (Exception e) {
            log.debug("序列化请求参数失败: {}", e.getMessage());
            return null;
        }
    }

    private int resolveHttpStatus(Throwable ex) {
        if (ex instanceof AdminModuleException ame) {
            return ame.getErrorCode().getHttpStatus();
        }
        if (ex instanceof com.qiwenshare.auth.exception.AuthException ae) {
            return ae.getErrorCode().getHttpStatus();
        }
        return 500;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个 IP（多代理场景）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
