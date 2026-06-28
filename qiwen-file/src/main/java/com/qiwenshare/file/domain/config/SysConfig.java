package com.qiwenshare.file.domain.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_config")
@TableName("sys_config")
public class SysConfig {
    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String configKey;
    private String configValue;
    private String description;
}
