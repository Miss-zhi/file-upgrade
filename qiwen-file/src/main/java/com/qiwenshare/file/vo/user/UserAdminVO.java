package com.qiwenshare.file.vo.user;

import com.qiwenshare.file.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 管理端用户出参
 */
@Data
@Builder
@Schema(description = "管理端用户信息")
public class UserAdminVO {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private String createTime;

    public static UserAdminVO fromEntity(User user) {
        return UserAdminVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createTime(user.getCreateTime() != null ? user.getCreateTime().toString() : "")
                .build();
    }
}
