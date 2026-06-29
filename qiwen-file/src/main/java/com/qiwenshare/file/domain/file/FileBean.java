package com.qiwenshare.file.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件实体
 */
@Data
@Entity
@Table(name = "file_bean")
@TableName("file_bean")
public class FileBean {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 文件名 */
    private String fileName;

    /** 完整路径，如 /docs/note.txt */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型（MIME 或扩展名） */
    private String fileType;

    /** 是否为文件夹 */
    private Boolean isFolder;

    /** 父目录路径 */
    private String parentPath;

    /** 拥有者用户 ID */
    private String userId;

    /** 逻辑删除标记 (0=正常, 1=已删除) */
    private Integer deleted = 0;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
