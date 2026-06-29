package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.dto.user.UserQueryDTO;
import com.qiwenshare.file.dto.user.UserUpdateDTO;
import com.qiwenshare.file.service.SysConfigService;
import com.qiwenshare.file.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.user.UserAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端控制器
 */
@Tag(name = "管理端", description = "用户管理")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final IUserService userService;
    private final StatsService statsService;
    private final SysConfigService sysConfigService;

    @Operation(summary = "分页搜索用户列表")
    @PostMapping("/user/list")
    public RestResult<Map<String, Object>> listUsers(@RequestBody UserQueryDTO dto) {
        IPage<User> page = userService.listUsers(dto.getPage(), dto.getSize(), dto.getKeyword());
        List<UserAdminVO> records = page.getRecords().stream().map(UserAdminVO::fromEntity).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("size", page.getSize());
        result.put("current", page.getCurrent());
        return RestResult.success(result);
    }

    @Operation(summary = "更新用户")
    @MyLog(module = "管理端", value = "更新用户")
    @PutMapping("/user")
    public RestResult<Void> updateUser(@RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto.getId(), dto.getEmail(), dto.getPhone(), dto.getNickname(), dto.getAvatar());
        return RestResult.success();
    }

    @Operation(summary = "删除用户")
    @MyLog(module = "管理端", value = "删除用户")
    @DeleteMapping("/user/{id}")
    public RestResult<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return RestResult.success();
    }

    @Operation(summary = "切换用户状态")
    @MyLog(module = "管理端", value = "切换用户状态")
    @PutMapping("/user/{id}/status")
    public RestResult<Void> toggleStatus(@PathVariable String id, @RequestParam boolean enabled) {
        userService.toggleStatus(id, enabled);
        return RestResult.success();
    }

    @Operation(summary = "统计信息")
    @GetMapping("/stats")
    public RestResult<Map<String, Object>> getStats() {
        return RestResult.success(statsService.getStats());
    }

    @Operation(summary = "获取系统配置")
    @GetMapping("/config")
    public RestResult<Map<String, String>> getConfig() {
        return RestResult.success(sysConfigService.getAllConfig());
    }

    @Operation(summary = "保存系统配置")
    @PutMapping("/config")
    public RestResult<Void> saveConfig(@RequestBody Map<String, String> config) {
        sysConfigService.saveConfig(config);
        return RestResult.success();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/user/{id}/role")
    public RestResult<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        userService.updateRole(id, body.get("role"));
        return RestResult.success();
    }
}
