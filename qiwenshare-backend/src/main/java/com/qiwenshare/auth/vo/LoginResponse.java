package com.qiwenshare.auth.vo;

import java.util.List;

/** 登录响应 VO。 */
public record LoginResponse(
        String userId,
        String username,
        List<String> roles,
        List<String> permissions
) {}
