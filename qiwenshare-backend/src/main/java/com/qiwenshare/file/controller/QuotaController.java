package com.qiwenshare.file.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.service.StorageQuotaService;
import com.qiwenshare.file.vo.QuotaInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 存储配额控制器。
 *
 * <p>提供用户侧配额查询端点。管理员配额管理由 admin-module 的 AdminQuotaController 提供。</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuotaController {

    private final StorageQuotaService storageQuotaService;

    /**
     * 查询当前用户配额信息。
     *
     * @param authentication 当前认证信息
     * @return 配额信息
     */
    @GetMapping("/quota/info")
    public RestResult<QuotaInfoVO> getQuotaInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuotaInfoVO quotaInfo = storageQuotaService.getQuotaInfo(userId);
        return RestResult.success(quotaInfo);
    }
}
