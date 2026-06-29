package com.qiwenshare.file.domain.task;

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
@Table(name = "upload_task")
@TableName("upload_task")
public class UploadTask {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String identifier;
    private String userId;
    private String fileName;
    private String filePath;
    private Long totalSize;
    private Integer chunkNum;
    private Integer totalChunks;
    private Integer uploadStatus;
    private LocalDateTime createTime;
}
