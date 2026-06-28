package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.dto.user.LoginDTO;
import com.qiwenshare.file.dto.user.RegisterDTO;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "登录/注册/用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "用户登录")
    @MyLog(module = "用户管理", value = "用户登录")
    @PostMapping("/login")
    public RestResult<String> login(@Valid @RequestBody LoginDTO dto) {
        String token = userService.login(dto.getUsername(), dto.getPassword());
        return RestResult.success(token);
    }

    @Operation(summary = "用户注册")
    @MyLog(module = "用户管理", value = "用户注册")
    @PostMapping("/register")
    public RestResult<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto.getUsername(), dto.getPassword(), dto.getEmail());
        return RestResult.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public RestResult<UserVO> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        User user = userService.getUserById(userId);
        return RestResult.success(UserVO.fromUser(user));
    }
}
