package com.qiwenshare.file.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "permission")
@TableName("permission")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long permissionId;

    private Long parentId;
    private String permissionName;
    private Integer resourceType;
    private String permissionCode;
    private Integer orderNum;
}
