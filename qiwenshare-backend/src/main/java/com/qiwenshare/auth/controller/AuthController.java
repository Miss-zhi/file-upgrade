package com.qiwenshare.auth.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.auth.dto.ChangePasswordRequest;
import com.qiwenshare.auth.dto.LoginRequest;
import com.qiwenshare.auth.dto.RegisterRequest;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.vo.LoginResponse;
import com.qiwenshare.auth.vo.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证相关 REST 端点。
 *
 * <p>所有端点统一在 {@code /api/v1/auth} 前缀下。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册。
     *
     * @param request 注册请求体
     * @return 注册成功返回 userId
     */
    @PostMapping("/register")
    public ResponseEntity<RestResult<Map<String, String>>> register(@Valid @RequestBody RegisterRequest request) {
        String userId = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResult.success("注册成功", Map.of("userId", userId)));
    }

    /**
     * 用户登录。
     *
     * @param request  登录请求体
     * @param response HTTP 响应（设置 cookie）
     * @return 登录响应
     */
    @PostMapping("/login")
    public ResponseEntity<RestResult<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(RestResult.success("登录成功", loginResponse));
    }

    /**
     * Token 刷新。
     *
     * @param request  HTTP 请求（从 cookie 提取 refresh token）
     * @param response HTTP 响应（设置新 cookie）
     * @return 刷新成功响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<RestResult<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.refresh(request, response);
        return ResponseEntity.ok(RestResult.success("Token 已刷新"));
    }

    /**
     * 用户登出。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @return 登出成功响应
     */
    @PostMapping("/logout")
    public ResponseEntity<RestResult<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(RestResult.success("已登出"));
    }

    /**
     * 获取当前用户信息。
     *
     * @return 用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<RestResult<UserInfoResponse>> me() {
        UserInfoResponse userInfo = authService.getCurrentUser();
        return ResponseEntity.ok(RestResult.success(userInfo));
    }

    /**
     * 修改密码。
     *
     * @param request 修改密码请求体
     * @return 修改成功响应
     */
    @PutMapping("/password")
    public ResponseEntity<RestResult<Void>> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.updatePassword(request);
        return ResponseEntity.ok(RestResult.success("密码修改成功，请重新登录"));
    }
}
