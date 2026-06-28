package com.qiwenshare.file.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户信息出参
 */
@Data
@Builder
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID", example = "1234567890")
    private String id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "邮箱", example = "admin@qiwenshare.com")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    public static UserVO fromUser(com.qiwenshare.file.domain.user.User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }
}
