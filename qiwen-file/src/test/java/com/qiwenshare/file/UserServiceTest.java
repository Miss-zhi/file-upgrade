package com.qiwenshare.file;

import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.exception.QiwenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private IUserService userService;

    @Test
    @DisplayName("注册新用户成功")
    void testRegisterSuccess() {
        userService.register("testuser", "123456", "test@qiwenshare.com");

        var user = userService.findByUsername("testuser");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@qiwenshare.com", user.getEmail());
        // 密码应为加密后的密文，不是明文
        assertNotEquals("123456", user.getPassword());
    }

    @Test
    @DisplayName("重复用户名注册失败")
    void testRegisterDuplicate() {
        userService.register("duplicate", "123456", "dup@test.com");

        assertThrows(QiwenException.class, () ->
                userService.register("duplicate", "654321", "dup2@test.com")
        );
    }

    @Test
    @DisplayName("登录成功返回 Token")
    void testLoginSuccess() {
        userService.register("loginuser", "mypassword", "login@test.com");

        String token = userService.login("loginuser", "mypassword");
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    @Test
    @DisplayName("密码错误登录失败")
    void testLoginWrongPassword() {
        userService.register("wrongpw", "correct", "wrong@test.com");

        assertThrows(QiwenException.class, () ->
                userService.login("wrongpw", "wrongpassword")
        );
    }

    @Test
    @DisplayName("用户不存在登录失败")
    void testLoginUserNotFound() {
        assertThrows(QiwenException.class, () ->
                userService.login("nobody", "anypassword")
        );
    }

    @Test
    @DisplayName("根据 ID 获取用户")
    void testGetUserById() {
        userService.register("iduser", "123456", "id@test.com");
        var user = userService.findByUsername("iduser");

        var found = userService.getUserById(user.getId());
        assertNotNull(found);
        assertEquals("iduser", found.getUsername());
    }
}
