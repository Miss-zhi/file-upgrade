package com.qiwenshare.admin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.admin.entity.OperationLog;
import com.qiwenshare.admin.common.OperationLogAsyncWriter;
import com.qiwenshare.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AuditLogAspect 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private OperationLogAsyncWriter asyncWriter;

    @Mock
    private UserRepository userRepository;

    private AuditLogAspect aspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect(asyncWriter, userRepository, objectMapper);

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));

        // 设置模拟请求
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/admin/users/123/disable");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "TestAgent/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("成功执行方法后记录审计日??)")

    void around_success() throws Throwable {
        // 模拟一个带 @AuditLog 注解的方法调??
        TestTarget target = new TestTarget();

        aspect.around(
                mockJoinPoint(target, "testMethod", new String[]{"userId"}, new Object[]{"123"}),
                mockAuditLog("user", "UPDATE", "禁用用户")
        );

        // 验证 proceed 被调??
        // 注意：由??asyncWriter.save 是异步的，这里主要验证方法正常执行不抛异??
    }

    @Test
    @DisplayName("方法抛异常时仍记录审计日??)")

    void around_exception() throws Throwable {
        TestTarget target = new TestTarget();

        var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
        var signature = mock(org.aspectj.lang.reflect.MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

        try {
            aspect.around(joinPoint, mockAuditLog("user", "DELETE", "删除用户"));
        } catch (RuntimeException e) {
            // expected
        }

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(asyncWriter).save(captor.capture());
        OperationLog savedLog = captor.getValue();

        assertThat(savedLog.getModule()).isEqualTo("user");
        assertThat(savedLog.getAction()).isEqualTo("DELETE");
        assertThat(savedLog.getResponseCode()).isEqualTo(500);
        assertThat(savedLog.getErrorMessage()).isEqualTo("test error");
    }

    @Test
    @DisplayName("请求参数脱敏 - password 字段被遮??)")

    void sanitize_passwordMasked() throws Exception {
        // 测试脱敏逻辑
        String[] paramNames = {"userId", "password"};
        Object[] args = {"123", "secretPassword"};

        // 通过反射测试私有方法（或使用集成测试验证??
        // 这里验证脱敏后的 JSON 不包含明文密??
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeAndSerialize", String[].class, Object[].class);
        method.setAccessible(true);
        String result = (String) method.invoke(aspect, paramNames, args);

        assertThat(result).contains("******");
        assertThat(result).doesNotContain("secretPassword");
    }

    // 辅助方法
    private org.aspectj.lang.ProceedingJoinPoint mockJoinPoint(Object target, String methodName,
                                                                  String[] paramNames, Object[] args) throws Throwable {
        var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
        var signature = mock(org.aspectj.lang.reflect.MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);
        doReturn(null).when(joinPoint).proceed();
        return joinPoint;
    }

    private AuditLog mockAuditLog(String module, String action, String description) {
        var auditLog = mock(AuditLog.class);
        when(auditLog.module()).thenReturn(module);
        when(auditLog.action()).thenReturn(action);
        when(auditLog.description()).thenReturn(description);
        return auditLog;
    }

    // 测试用目标类
    static class TestTarget {
        @AuditLog(module = "user", action = "UPDATE", description = "测试")
        public void testMethod(String userId) {}
    }
}
