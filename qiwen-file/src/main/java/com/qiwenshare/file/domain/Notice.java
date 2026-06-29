package com.qiwenshare.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "notice")
@TableName("notice")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private Integer platform;
    private String markdownContent;
    private String content;
    private String validDateTime;
    private Integer isLongValidData;
    private String createTime;
    private String createUserId;
    private String modifyTime;
    private String modifyUserId;
}
