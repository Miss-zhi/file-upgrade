package com.qiwenshare.file.service;

import com.qiwenshare.file.dto.SaveShareFileDTO;
import com.qiwenshare.file.dto.ShareCreateDTO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.ShareFile;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.ShareFileRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.file.vo.ShareInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件分享服务。
 *
 * <p>处理创建分享、验证提取码、查看分享、取消分享。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileShareService {

    private static final String SHARE_CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String EXTRACT_CODE_CHARS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ShareFileRepository shareFileRepository;
    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;

    /**
     * 创建分享链接。
     *
     * @param dto    创建分享请求
     * @param userId 用户 ID
     * @return 分享信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ShareInfoVO createShare(ShareCreateDTO dto, Long userId) {
        UserFile userFile = userFileRepository.findById(dto.userFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        if (!userFile.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }

        ShareFile shareFile = new ShareFile();
        shareFile.setUserId(userId);
        shareFile.setUserFileId(dto.userFileId());
        shareFile.setShareCode(generateShareCode());

        // 设置提取码：shareType=1 时设置，优先使用客户端提供的 extractCode，否则随机生成
        if (dto.shareType() == null || dto.shareType() == 1) {
            if (dto.extractCode() != null && !dto.extractCode().isBlank()) {
                shareFile.setExtractCode(dto.extractCode().trim().toUpperCase());
            } else {
                shareFile.setExtractCode(generateExtractCode());
            }
        }

        // 设置过期时间：优先使用自定义 expireTime，其次使用 expireType 计算
        if (dto.expireTime() != null && !dto.expireTime().isBlank()) {
            shareFile.setExpireTime(LocalDateTime.parse(dto.expireTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else if (dto.expireType() != null && dto.expireType() > 0) {
            shareFile.setExpireTime(LocalDateTime.now().plusDays(dto.expireType()));
        }
        // expireType==0 或两者都为空 → 永久有效（expireTime 保持 null）

        shareFileRepository.save(shareFile);

        Long fileSize = 0L;
        if (userFile.getFileId() != null) {
            fileSize = fileBeanRepository.findById(userFile.getFileId())
                    .map(FileBean::getFileSize)
                    .orElse(0L);
        }

        String fullName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");

        return new ShareInfoVO(
                shareFile.getShareId(), shareFile.getUserFileId(),
                shareFile.getShareCode(), shareFile.getExtractCode(),
                shareFile.getExpireTime(), fullName, fileSize,
                shareFile.getViewCount(), shareFile.getCreateTime()
        );
    }

    /**
     * 验证提取码并获取分享信息（带 Redis 缓存）。
     *
     * @param shareCode   分享码
     * @param extractCode 提取码
     * @return 分享信息
     */
    public ShareInfoVO verifyShare(String shareCode, String extractCode) {
        // 检查 Redis 缓存是否已验证
        String cacheKey = "file:share:verified:" + shareCode;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // 已验证过，直接返回分享信息
            return getShareInfo(shareCode);
        }

        ShareFile shareFile = getValidShare(shareCode);

        // 验证提取码
        if (shareFile.getExtractCode() != null && !shareFile.getExtractCode().equals(extractCode)) {
            throw new FileModuleException(FileErrorCode.SHARE_EXTRACT_CODE_WRONG);
        }

        // 增加浏览次数
        shareFile.setViewCount(shareFile.getViewCount() + 1);
        shareFileRepository.save(shareFile);

        // 缓存验证状态 30 分钟
        redisTemplate.opsForValue().set(cacheKey, "1", 30, TimeUnit.MINUTES);

        return buildShareInfoVO(shareFile);
    }

    /**
     * 获取分享文件信息（验证后下载用）。
     *
     * @param shareCode 分享码
     * @return 分享关联的 UserFile
     */
    public UserFile getShareFile(String shareCode) {
        ShareFile shareFile = getValidShare(shareCode);
        return userFileRepository.findById(shareFile.getUserFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));
    }

    /**
     * 查看我的分享列表。
     *
     * @param userId 用户 ID
     * @return 分享列表
     */
    public List<ShareInfoVO> listMyShares(Long userId) {
        List<ShareFile> shares = shareFileRepository.findByUserId(userId);
        return shares.stream()
                .map(this::buildShareInfoVO)
                .collect(Collectors.toList());
    }

    /**
     * 取消分享。
     *
     * @param shareId 分享 ID
     * @param userId  用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long shareId, Long userId) {
        ShareFile shareFile = shareFileRepository.findById(shareId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.SHARE_NOT_FOUND));

        if (!shareFile.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }

        shareFileRepository.delete(shareFile);
    }

    /**
     * 清理过期分享记录（定时任务调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredShares() {
        List<ShareFile> expired = shareFileRepository.findByExpireTimeIsNotNullAndExpireTimeBefore(LocalDateTime.now());
        log.info("清理过期分享记录: {} 个", expired.size());
        shareFileRepository.deleteAll(expired);
    }

    /**
     * 保存分享文件到用户网盘。
     *
     * @param dto    保存请求
     * @param userId 当前登录用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveShareFile(SaveShareFileDTO dto, Long userId) {
        // 1. 验证分享有效性
        ShareFile shareFile = getValidShare(dto.shareCode());

        // 2. 获取源文件信息
        UserFile sourceUserFile = userFileRepository.findById(shareFile.getUserFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        // 3. 确定目标路径
        String targetPath;
        if (dto.targetNodeId() == null) {
            targetPath = "/";
        } else {
            UserFile targetFolder = userFileRepository.findById(dto.targetNodeId())
                    .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

            if (!targetFolder.getUserId().equals(userId)) {
                throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
            }

            // fileType=2 表示文件夹
            if (targetFolder.getFileType() != 2) {
                throw new FileModuleException(FileErrorCode.FOLDER_NOT_FOUND);
            }

            targetPath = targetFolder.getFilePath();
            // filePath 字段存储的是父目录路径，需要拼接文件名得到自身完整路径
            if (targetPath == null || targetPath.equals("/")) {
                targetPath = "/" + targetFolder.getFileName();
            } else {
                targetPath = targetPath + "/" + targetFolder.getFileName();
            }
        }

        // 4. 构造新文件的文件名
        String fileName = sourceUserFile.getFileName();
        String extendName = sourceUserFile.getExtendName();

        // 5. 检查文件名冲突，自动重命名
        String finalFileName = generateUniqueFileName(targetPath, fileName, extendName, userId);

        // 6. 创建新的 UserFile 记录
        UserFile newUserFile = new UserFile();
        newUserFile.setUserId(userId);
        newUserFile.setFileId(sourceUserFile.getFileId());
        newUserFile.setFileName(finalFileName);
        newUserFile.setExtendName(extendName);
        newUserFile.setFileType(sourceUserFile.getFileType());
        newUserFile.setFilePath(targetPath);
        newUserFile.setDeleteStatus(0); // 0=正常

        userFileRepository.save(newUserFile);

        log.info("用户 {} 保存分享文件 {} 到 {}", userId, finalFileName, targetPath);
    }

    /**
     * 生成不重复的文件名（如果已存在则追加序号）。
     * 注意：fileName 已包含扩展名（如 "main.py"），不需要再拼接 extendName。
     */
    private String generateUniqueFileName(String parentPath, String fileName, String extendName, Long userId) {
        // 检查是否已存在同名文件
        boolean exists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatus(
                userId, parentPath, fileName, extendName, 0);

        if (!exists) {
            return fileName;
        }

        // 追加序号直到找到可用名称
        int counter = 1;
        while (true) {
            // 从文件名中分离主名和扩展名来生成序号名称
            String baseName;
            String ext;
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = fileName.substring(0, dotIndex);
                ext = fileName.substring(dotIndex); // 包含 "."
            } else {
                baseName = fileName;
                ext = "";
            }
            String newName = baseName + "_" + counter + ext;
            boolean nameExists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatus(
                    userId, parentPath, newName, extendName, 0);
            if (!nameExists) {
                return newName;
            }
            counter++;
        }
    }

    private ShareFile getValidShare(String shareCode) {
        ShareFile shareFile = shareFileRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.SHARE_NOT_FOUND));

        // 检查过期
        if (shareFile.getExpireTime() != null && shareFile.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new FileModuleException(FileErrorCode.SHARE_EXPIRED);
        }

        return shareFile;
    }

    /**
     * 查看分享文件信息（公开端点，无需提取码）。
     *
     * @param shareCode 分享码
     * @return 分享信息
     */
    public ShareInfoVO getShareInfo(String shareCode) {
        ShareFile shareFile = getValidShare(shareCode);
        return buildShareInfoVO(shareFile);
    }

    private ShareInfoVO buildShareInfoVO(ShareFile shareFile) {
        UserFile userFile = userFileRepository.findById(shareFile.getUserFileId()).orElse(null);
        Long fileSize = 0L;
        String fullName = "文件已删除";

        if (userFile != null) {
            if (userFile.getFileId() != null) {
                fileSize = fileBeanRepository.findById(userFile.getFileId())
                        .map(FileBean::getFileSize)
                        .orElse(0L);
            }
            fullName = userFile.getFileName()
                    + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                            ? "." + userFile.getExtendName() : "");
        }

        return new ShareInfoVO(
                shareFile.getShareId(), shareFile.getUserFileId(),
                shareFile.getShareCode(), shareFile.getExtractCode(),
                shareFile.getExpireTime(), fullName, fileSize,
                shareFile.getViewCount(), shareFile.getCreateTime()
        );
    }

    private String generateShareCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(SHARE_CODE_CHARS.charAt(RANDOM.nextInt(SHARE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateExtractCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(EXTRACT_CODE_CHARS.charAt(RANDOM.nextInt(EXTRACT_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
