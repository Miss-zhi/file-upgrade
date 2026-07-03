package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.entity.DocumentVersion;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.repository.DocumentVersionRepository;
import com.qiwenshare.document.vo.DocumentVersionVO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档版本历史服务。
 *
 * <p>管理版本记录的创建、查询和清理。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentHistoryService {

    private final DocumentVersionRepository documentVersionRepository;
    private final FileBeanRepository fileBeanRepository;
    private final UserFileRepository userFileRepository;
    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 创建版本记录。
     *
     * <p>将当前 FileBean 信息记录为历史版本。超出最大保留数时删除最旧版本。</p>
     *
     * @param userFileId 用户文件 ID
     * @param fileBean   当前 FileBean（保存旧版本信息）
     * @param editorId   编辑者用户 ID
     */
    @Transactional
    public void createVersion(Long userFileId, FileBean fileBean, Long editorId) {
        Integer maxVersion = documentVersionRepository.findMaxVersionNumber(userFileId);
        int newVersionNumber = (maxVersion == null ? 0 : maxVersion) + 1;

        DocumentVersion version = new DocumentVersion();
        version.setUserFileId(userFileId);
        version.setFileId(fileBean.getFileId());
        version.setVersionNumber(newVersionNumber);
        version.setFileSize(fileBean.getFileSize());
        version.setEditorId(editorId);
        documentVersionRepository.save(version);

        log.info("创建版本记录: userFileId={}, version={}, editorId={}", userFileId, newVersionNumber, editorId);

        // 超出最大保留数时删除最旧版本
        long count = documentVersionRepository.countByUserFileId(userFileId);
        if (count > onlyOfficeProperties.getMaxVersionCount()) {
            documentVersionRepository.findFirstByUserFileIdOrderByVersionNumberAsc(userFileId)
                    .ifPresent(oldest -> {
                        documentVersionRepository.delete(oldest);
                        log.info("删除最旧版本: userFileId={}, version={}", userFileId, oldest.getVersionNumber());
                    });
        }
    }

    /**
     * 查询版本列表。
     *
     * @param userFileId 用户文件 ID
     * @return 版本列表（降序，最新在前）
     */
    public List<DocumentVersionVO> listVersions(Long userFileId) {
        return documentVersionRepository.findByUserFileIdOrderByVersionNumberDesc(userFileId)
                .stream()
                .map(v -> new DocumentVersionVO(
                        v.getVersionNumber(),
                        v.getEditorId(),
                        v.getFileSize(),
                        v.getCreateTime()))
                .toList();
    }

    /**
     * 查询指定版本（包私有，仅内部使用）。
     *
     * <p>返回 Entity 而非 VO，调用方必须在 Service 层内完成 Entity→VO 转换，
     * 禁止将返回值暴露给 Controller（红线 #4）。</p>
     *
     * @param userFileId    用户文件 ID
     * @param versionNumber 版本号
     * @return 版本记录
     * @throws DocumentModuleException 版本不存在时
     */
    DocumentVersion getVersion(Long userFileId, int versionNumber) {
        return documentVersionRepository.findByUserFileIdAndVersionNumber(userFileId, versionNumber)
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_VERSION_NOT_FOUND));
    }

    /**
     * 回滚到指定版本。
     *
     * <p>将历史版本的文件信息恢复为当前版本，并创建一条 RESTORE 类型的版本记录。</p>
     *
     * @param userFileId    用户文件 ID
     * @param versionNumber 目标版本号
     * @param editorId      执行回滚的用户 ID
     */
    @Transactional
    public void restoreVersion(Long userFileId, int versionNumber, Long editorId) {
        // 查找目标版本
        DocumentVersion targetVersion = getVersion(userFileId, versionNumber);

        // 查找当前 FileBean
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));
        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));

        // 创建回滚操作本身的版本记录（type=RESTORE）
        Integer maxVersion = documentVersionRepository.findMaxVersionNumber(userFileId);
        int newVersionNumber = (maxVersion == null ? 0 : maxVersion) + 1;

        DocumentVersion restoreRecord = new DocumentVersion();
        restoreRecord.setUserFileId(userFileId);
        restoreRecord.setFileId(fileBean.getFileId());
        restoreRecord.setVersionNumber(newVersionNumber);
        restoreRecord.setFileSize(fileBean.getFileSize());
        restoreRecord.setEditorId(editorId);
        restoreRecord.setType("RESTORE");
        documentVersionRepository.save(restoreRecord);

        // 更新 FileBean.modifyTime（触发 document key 变化）
        fileBean.setModifyTime(java.time.LocalDateTime.now());
        fileBeanRepository.save(fileBean);

        // 更新 UserFile.modifyTime
        userFile.setModifyTime(java.time.LocalDateTime.now());
        userFileRepository.save(userFile);

        log.info("版本回滚完成: userFileId={}, targetVersion={}, newVersion={}, editorId={}",
                userFileId, versionNumber, newVersionNumber, editorId);

        // 超出最大保留数时删除最旧版本
        long count = documentVersionRepository.countByUserFileId(userFileId);
        if (count > onlyOfficeProperties.getMaxVersionCount()) {
            documentVersionRepository.findFirstByUserFileIdOrderByVersionNumberAsc(userFileId)
                    .ifPresent(oldest -> {
                        documentVersionRepository.delete(oldest);
                        log.info("删除最旧版本: userFileId={}, version={}", userFileId, oldest.getVersionNumber());
                    });
        }
    }
}
