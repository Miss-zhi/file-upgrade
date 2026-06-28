package com.qiwenshare.file.domain.share;

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
@Table(name = "share_file")
@TableName("share_file")
public class ShareFile {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String shareBatchNum;

    private String userId;

    private String filePath;

    private String shareToken;

    private String shareCode;

    private Integer expireDays;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
