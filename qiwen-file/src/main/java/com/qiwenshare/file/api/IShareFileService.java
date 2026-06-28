package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.share.ShareFile;
import java.util.List;

public interface IShareFileService {

    ShareFile createShare(String filePath, String userId, Integer expireDays, String shareCode);

    List<ShareFile> listShares(String userId);

    void cancelShare(String shareId, String userId);

    ShareFile verifyShare(String token, String code);

    ShareFile getShareByToken(String token);
}
