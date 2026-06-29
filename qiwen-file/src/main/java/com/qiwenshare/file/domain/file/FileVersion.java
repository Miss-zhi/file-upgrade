package com.qiwenshare.file.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "file_version")
@TableName("file_version")
public class FileVersion {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String fileId;
    private Integer version;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String storagePath;
    private String userId;
    private LocalDateTime createTime;
}
