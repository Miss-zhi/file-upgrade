package com.qiwenshare.file.vo.share;

import com.qiwenshare.file.domain.share.ShareFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "分享信息")
public class ShareVO {

    private String id;
    private String shareToken;
    private String shareCode;
    private String filePath;
    private Integer expireDays;
    private String expireTime;
    private String createTime;
    private String link;

    public static ShareVO fromEntity(ShareFile share, String link) {
        return ShareVO.builder()
                .id(share.getId())
                .shareToken(share.getShareToken())
                .shareCode(share.getShareCode())
                .filePath(share.getFilePath())
                .expireDays(share.getExpireDays())
                .expireTime(share.getExpireTime() != null ? share.getExpireTime().toString() : "")
                .createTime(share.getCreateTime() != null ? share.getCreateTime().toString() : "")
                .link(link)
                .build();
    }
}
