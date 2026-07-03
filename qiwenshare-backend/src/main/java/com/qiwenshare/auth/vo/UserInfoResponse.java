package com.qiwenshare.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 用户信息响应 VO（手机号脱敏）。 */
public record UserInfoResponse(
        String userId,
        String username,
        String telephone,
        String avatar,
        List<String> roles,
        List<String> permissions,
        LocalDateTime registerTime
) {}
