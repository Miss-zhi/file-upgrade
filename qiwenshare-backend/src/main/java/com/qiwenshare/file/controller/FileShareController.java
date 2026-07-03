package com.qiwenshare.file.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.dto.SaveShareFileDTO;
import com.qiwenshare.file.dto.ShareCreateDTO;
import com.qiwenshare.file.dto.ShareVerifyDTO;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.service.FileDownloadService;
import com.qiwenshare.file.service.FileShareService;
import com.qiwenshare.file.vo.ShareInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件分享控制器。
 *
 * <p>处理分享链接创建、验证、下载、管理。</p>
 */
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService fileShareService;
    private final FileDownloadService fileDownloadService;

    /**
     * 创建分享链接。
     *
     * @param dto            创建分享请求
     * @param authentication 当前认证信息
     * @return 分享信息
     */
    @PostMapping("/createshare")
    public RestResult<ShareInfoVO> createShare(
            @Valid @RequestBody ShareCreateDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ShareInfoVO result = fileShareService.createShare(dto, userId);
        return RestResult.success(result);
    }

    /**
     * 查看分享内容（公开端点，无需认证）。
     *
     * @param shareCode 分享码
     * @return 分享信息
     */
    @GetMapping("/info/{shareCode}")
    public RestResult<ShareInfoVO> getShareInfo(@PathVariable String shareCode) {
        ShareInfoVO result = fileShareService.getShareInfo(shareCode);
        return RestResult.success(result);
    }

    /**
     * 验证提取码。
     *
     * @param dto 验证请求
     * @return 分享信息
     */
    @PostMapping("/verifyshare")
    public RestResult<ShareInfoVO> verifyShare(@Valid @RequestBody ShareVerifyDTO dto) {
        ShareInfoVO result = fileShareService.verifyShare(dto.shareCode(), dto.extractCode());
        return RestResult.success(result);
    }

    /**
     * 分享文件下载。
     *
     * @param shareCode 分享码
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     */
    @GetMapping("/download/{shareCode}")
    public void downloadSharedFile(
            @PathVariable String shareCode,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        UserFile userFile = fileShareService.getShareFile(shareCode);
        Long downloaderUserId = authentication != null ? Long.parseLong(authentication.getName()) : null;
        fileDownloadService.downloadForShare(userFile.getUserFileId(), request, response, downloaderUserId);
    }

    /**
     * 我的分享列表。
     *
     * @param authentication 当前认证信息
     * @return 分享列表
     */
    @GetMapping("/myshares")
    public RestResult<List<ShareInfoVO>> listMyShares(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ShareInfoVO> result = fileShareService.listMyShares(userId);
        return RestResult.success(result);
    }

    /**
     * 取消分享。
     *
     * @param shareId        分享 ID
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @DeleteMapping("/cancelshare/{shareId}")
    public RestResult<Void> cancelShare(
            @PathVariable Long shareId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileShareService.cancelShare(shareId, userId);
        return RestResult.success("取消分享成功");
    }

    /**
     * 保存分享文件到网盘。
     *
     * @param dto            保存请求（包含分享码和目标目录）
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/saveshare")
    public RestResult<Void> saveShareFile(
            @Valid @RequestBody SaveShareFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileShareService.saveShareFile(dto, userId);
        return RestResult.success("保存成功");
    }
}
