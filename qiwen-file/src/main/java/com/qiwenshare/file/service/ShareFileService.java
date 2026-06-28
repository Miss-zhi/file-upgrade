package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.domain.share.ShareFile;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.ShareFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareFileService extends ServiceImpl<ShareFileMapper, ShareFile> implements IShareFileService {

    private final ShareFileMapper shareFileMapper;

    @Override
    @Transactional
    public ShareFile createShare(String filePath, String userId, Integer expireDays, String shareCode) {
        ShareFile share = new ShareFile();
        share.setId(IdUtil.getSnowflakeNextIdStr());
        share.setShareBatchNum(IdUtil.getSnowflakeNextIdStr());
        share.setUserId(userId);
        share.setFilePath(filePath);
        share.setShareToken(RandomUtil.randomString(16));
        share.setShareCode(shareCode != null ? shareCode : RandomUtil.randomNumbers(4));
        share.setExpireDays(expireDays != null ? expireDays : 7);
        share.setExpireTime(LocalDateTime.now().plusDays(share.getExpireDays()));
        share.setCreateTime(LocalDateTime.now());
        shareFileMapper.insert(share);
        return share;
    }

    @Override
    public List<ShareFile> listShares(String userId) {
        LambdaQueryWrapper<ShareFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareFile::getUserId, userId)
               .orderByDesc(ShareFile::getCreateTime);
        return shareFileMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void cancelShare(String shareId, String userId) {
        ShareFile share = shareFileMapper.selectById(shareId);
        if (share == null || !share.getUserId().equals(userId)) {
            throw new QiwenException(403, "无权取消此分享");
        }
        shareFileMapper.deleteById(shareId);
    }

    @Override
    public ShareFile verifyShare(String token, String code) {
        ShareFile share = getShareByToken(token);
        if (share == null) throw new QiwenException(404, "分享不存在或已取消");
        if (share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new QiwenException(400, "分享已过期");
        }
        if (share.getShareCode() != null && !share.getShareCode().equals(code)) {
            throw new QiwenException(400, "提取码错误");
        }
        return share;
    }

    @Override
    public ShareFile getShareByToken(String token) {
        LambdaQueryWrapper<ShareFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareFile::getShareToken, token);
        return shareFileMapper.selectOne(wrapper);
    }
}
