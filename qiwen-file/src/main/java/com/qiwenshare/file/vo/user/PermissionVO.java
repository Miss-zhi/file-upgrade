package com.qiwenshare.file.vo.user;

import lombok.Data;
import java.util.List;

@Data
public class PermissionVO {
    private Long permissionId;
    private Long parentId;
    private String permissionName;
    private Integer resourceType;
    private String permissionCode;
    private Integer orderNum;
    private List<PermissionVO> children;
}
