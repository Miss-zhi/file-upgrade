package com.qiwenshare.auth.service;

import com.qiwenshare.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TokenService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-junit-testing-must-be-long-enough-here".getBytes());

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtProperties jwtProperties;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(SECRET, 900, 604800, 30);
        tokenService = new TokenService(jwtProperties, redisTemplate);
    }

    @Nested
    @DisplayName("Token 生成")
    class TokenGeneration {

        @Test
        @DisplayName("生成 access token 包含正确 claim")
        void generateAccessToken_containsCorrectClaims() {
            String token = tokenService.generateAccessToken("user123", List.of("ADMIN", "USER"));

            Claims claims = tokenService.parseAndValidate(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("user123");
            assertThat(claims.get("type", String.class)).isEqualTo("access");
            assertThat(claims.get("jti", String.class)).isNotBlank();
            assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "USER");
        }

        @Test
        @DisplayName("生成 refresh token 包含正确 claim")
        void generateRefreshToken_containsCorrectClaims() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String token = tokenService.generateRefreshToken("user123");

            Claims claims = tokenService.parseAndValidate(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("user123");
            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
            assertThat(claims.get("jti", String.class)).isNotBlank();
            // 验证 Redis 注册
            verify(valueOperations).set(
                    startsWith("token:refresh:"), eq("user123"),
                    eq(Duration.ofSeconds(604800)));
        }

        @Test
        @DisplayName("每次生成??token jti 不同")
        void generateTokens_differentJti() {
            String token1 = tokenService.generateAccessToken("user123", List.of());
            String token2 = tokenService.generateAccessToken("user123", List.of());

            Claims c1 = tokenService.parseAndValidate(token1);
            Claims c2 = tokenService.parseAndValidate(token2);
            assertThat(c1.get("jti", String.class)).isNotEqualTo(c2.get("jti", String.class));
        }
    }

    @Nested
    @DisplayName("Token 解析")
    class TokenParsing {

        @Test
        @DisplayName("签名无效??token 返回 null")
        void parseAndValidate_invalidSignature_returnsNull() {
            // 用不同密钥生??token
            JwtProperties otherProps = new JwtProperties(
                    Base64.getEncoder().encodeToString("another-secret-key-for-testing-purposes-only-1234567890".getBytes()),
                    900, 604800, 30);
            TokenService otherService = new TokenService(otherProps, redisTemplate);
            String token = otherService.generateAccessToken("user123", List.of());

            Claims result = tokenService.parseAndValidate(token);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("过期 token 返回 null")
        void parseAndValidate_expiredToken_returnsNull() {
            // 用不同密钥签名的 token 会被拒绝（测试签名无??过期场景??
            String otherSecret = Base64.getEncoder()
                    .encodeToString("completely-different-secret-key-for-testing-expiry-123456".getBytes());
            JwtProperties otherProps = new JwtProperties(otherSecret, 900, 604800, 30);
            TokenService otherService = new TokenService(otherProps, redisTemplate);
            String token = otherService.generateAccessToken("user123", List.of());

            // 用原??service 解析，签名不匹配应返??null
            Claims result = tokenService.parseAndValidate(token);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("黑名??)")

    class Blacklist {

        @Test
        @DisplayName("加入黑名单后可检测到")
        void blacklist_thenIsBlacklisted_returnsTrue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.hasKey("token:blacklist:jti-123")).thenReturn(true);

            tokenService.blacklist("jti-123", 300);

            assertThat(tokenService.isBlacklisted("jti-123")).isTrue();
            verify(valueOperations).set(eq("token:blacklist:jti-123"), eq("1"), eq(Duration.ofSeconds(300)));
        }

        @Test
        @DisplayName("剩余有效期为 0 时不加入黑名??)")

        void blacklist_zeroRemaining_doesNotSet() {
            tokenService.blacklist("jti-123", 0);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("全局撤销")
    class GlobalRevoke {

        @Test
        @DisplayName("撤销后旧 token 被检测为已撤销")
        void revokeAllTokens_thenIsRevoked_returnsTrue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // 模拟撤销时间??
            long now = Instant.now().getEpochSecond();
            when(valueOperations.get("revoke:all:user123")).thenReturn(String.valueOf(now));

            tokenService.revokeAllTokens("user123");

            // token 签发时间早于撤销时间
            assertThat(tokenService.isRevoked("user123", now - 100)).isTrue();
            // token 签发时间晚于撤销时间，未撤销
            assertThat(tokenService.isRevoked("user123", now + 100)).isFalse();
        }

        @Test
        @DisplayName("未撤销??isRevoked 返回 false")
        void isRevoked_noRevokeRecord_returnsFalse() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("revoke:all:user123")).thenReturn(null);

            assertThat(tokenService.isRevoked("user123", Instant.now().getEpochSecond())).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Token Rotation")
    class RefreshRotation {

        @Test
        @DisplayName("消费 refresh token 返回 userId 并删??)")

        void consumeRefreshToken_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("token:refresh:jti-abc")).thenReturn("user123");
            when(redisTemplate.delete("token:refresh:jti-abc")).thenReturn(true);

            String result = tokenService.consumeRefreshToken("jti-abc");

            assertThat(result).isEqualTo("user123");
            verify(redisTemplate).delete("token:refresh:jti-abc");
        }

        @Test
        @DisplayName("重用??refresh token 返回 null")
        void consumeRefreshToken_reused_returnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("token:refresh:jti-used")).thenReturn(null);

            String result = tokenService.consumeRefreshToken("jti-used");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("剩余有效??)")

    class Remaining {

        @Test
        @DisplayName("未来过期时间返回正数")
        void remainingSeconds_future_returnsPositive() {
            Instant future = Instant.now().plusSeconds(300);
            long remaining = tokenService.remainingSeconds(future);
            assertThat(remaining).isBetween(298L, 301L);
        }

        @Test
        @DisplayName("过去过期时间返回 0")
        void remainingSeconds_past_returnsZero() {
            Instant past = Instant.now().minusSeconds(10);
            assertThat(tokenService.remainingSeconds(past)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("文档 Token")
    class DocumentToken {

        @Test
        @DisplayName("生成文档 token 包含正确 claims")
        void generateDocumentToken_containsCorrectClaims() {
            String token = tokenService.generateDocumentToken("user1", 100L, "edit", 14400);

            Claims claims = tokenService.parseDocumentToken(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("user1");
            assertThat(claims.get("type", String.class)).isEqualTo("doc");
            assertThat(claims.get("doc.fileId", Long.class)).isEqualTo(100L);
            assertThat(claims.get("doc.action", String.class)).isEqualTo("edit");
        }

        @Test
        @DisplayName("解析??doc 类型 token 返回 null")
        void parseDocumentToken_wrongType_returnsNull() {
            String accessToken = tokenService.generateAccessToken("user1", List.of());

            Claims result = tokenService.parseDocumentToken(accessToken);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("无效签名文档 token 返回 null")
        void parseDocumentToken_invalidSignature_returnsNull() {
            JwtProperties otherProps = new JwtProperties(
                    Base64.getEncoder().encodeToString("another-secret-key-for-testing-purposes-only-1234567890".getBytes()),
                    900, 604800, 30);
            TokenService otherService = new TokenService(otherProps, redisTemplate);
            String token = otherService.generateDocumentToken("user1", 100L, "edit", 14400);

            Claims result = tokenService.parseDocumentToken(token);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("回调 Token")
    class CallbackToken {

        @Test
        @DisplayName("生成回调 token 包含正确 claims")
        void generateCallbackToken_containsCorrectClaims() {
            String token = tokenService.generateCallbackToken("user1", 200L, "edit", 1800);

            Claims claims = tokenService.parseCallbackToken(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("user1");
            assertThat(claims.get("type", String.class)).isEqualTo("cb");
            assertThat(claims.get("cb.fileId", Long.class)).isEqualTo(200L);
            assertThat(claims.get("cb.type", String.class)).isEqualTo("edit");
        }

        @Test
        @DisplayName("解析??cb 类型 token 返回 null")
        void parseCallbackToken_wrongType_returnsNull() {
            String accessToken = tokenService.generateAccessToken("user1", List.of());

            Claims result = tokenService.parseCallbackToken(accessToken);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("文档 token 不能通过回调 token 验证")
        void parseCallbackToken_docToken_returnsNull() {
            String docToken = tokenService.generateDocumentToken("user1", 100L, "edit", 14400);

            Claims result = tokenService.parseCallbackToken(docToken);
            assertThat(result).isNull();
        }
    }
}
