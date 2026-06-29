package com.qiwenshare.file;

import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.domain.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserRoleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserService userService;

    @Test
    @DisplayName("新注册用户默认角色为 USER")
    void testDefaultRoleIsUser() {
        userService.register("roleuser1", "123456", "role1@test.com");
        User user = userService.findByUsername("roleuser1");
        assertNotNull(user);
        assertEquals("USER", user.getRole());
    }

    @Test
    @DisplayName("登录返回的 Token 包含角色信息")
    void testLoginTokenHasRole() {
        userService.register("roleuser2", "123456", "role2@test.com");
        String token = userService.login("roleuser2", "123456");
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    @Test
    @DisplayName("更新用户角色")
    void testUpdateRole() {
        userService.register("roleuser3", "123456", "role3@test.com");
        User user = userService.findByUsername("roleuser3");

        userService.updateRole(user.getId(), "ADMIN");
        User updated = userService.getUserById(user.getId());
        assertEquals("ADMIN", updated.getRole());

        userService.updateRole(user.getId(), "USER");
        updated = userService.getUserById(user.getId());
        assertEquals("USER", updated.getRole());
    }

    @Test
    @DisplayName("无效角色更新抛出异常")
    void testUpdateInvalidRole() {
        userService.register("roleuser4", "123456", "role4@test.com");
        User user = userService.findByUsername("roleuser4");

        assertThrows(com.qiwenshare.file.exception.QiwenException.class, () ->
                userService.updateRole(user.getId(), "SUPER_ADMIN")
        );
    }

    @Test
    @DisplayName("ADMIN 角色可访问管理端点")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAccessGranted() throws Exception {
        mockMvc.perform(post("/admin/user/list")
                        .contentType("application/json")
                        .content("{\"page\":1,\"size\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER 角色访问管理端点被拒绝")
    @WithMockUser(username = "user", roles = {"USER"})
    void testUserAccessDenied() throws Exception {
        mockMvc.perform(post("/admin/user/list")
                        .contentType("application/json")
                        .content("{\"page\":1,\"size\":10}"))
                .andExpect(status().isForbidden());
    }
}
