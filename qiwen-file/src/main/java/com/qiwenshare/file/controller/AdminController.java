package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.file.api.IAdminService;
import com.qiwenshare.file.dto.user.ResetPasswordDTO;
import com.qiwenshare.file.dto.user.StorageUpdateDTO;
import com.qiwenshare.file.dto.user.UserAvailableDTO;
import com.qiwenshare.file.dto.user.UserSearchDTO;
import com.qiwenshare.file.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "admin", description = "该接口为管理员接口，主要做用户管理和存储管理")
@RestController
@Slf4j
@RequestMapping("/admin")
@PreAuthorize("hasRole('超级管理员')")
public class AdminController {

    @Resource
    IAdminService adminService;

    public static final String CURRENT_MODULE = "管理员模块";

    @Operation(summary = "查询用户列表", description = "查询用户列表", tags = {"admin"})
    @GetMapping("/user/list")
    public RestResult<IPage<UserVO>> getUserList(UserSearchDTO userSearchDTO) {
        return adminService.getUserList(userSearchDTO);
    }

    @Operation(summary = "修改用户状态", description = "修改用户状态", tags = {"admin"})
    @PostMapping("/user/updateAvailable")
    @MyLog(operation = "修改用户状态", module = CURRENT_MODULE)
    public RestResult<String> updateUserAvailable(@RequestBody UserAvailableDTO userAvailableDTO) {
        return adminService.updateUserAvailable(userAvailableDTO);
    }

    @Operation(summary = "修改用户存储空间", description = "修改用户存储空间", tags = {"admin"})
    @PostMapping("/storage/updateTotalStorage")
    @MyLog(operation = "修改用户存储空间", module = CURRENT_MODULE)
    public RestResult<String> updateUserStorage(@RequestBody StorageUpdateDTO storageUpdateDTO) {
        return adminService.updateUserStorage(storageUpdateDTO);
    }

    @Operation(summary = "重置用户密码", description = "重置用户密码", tags = {"admin"})
    @PostMapping("/user/resetPassword")
    @MyLog(operation = "重置用户密码", module = CURRENT_MODULE)
    public RestResult<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        return adminService.resetPassword(resetPasswordDTO);
    }
}
