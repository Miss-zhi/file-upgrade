package com.qiwenshare.file.service;

/**
 * 文件权限检查服务。
 *
 * <p>提供文件级权限判断，供 document 等外部模块调用。
 * 权限规则：文件所有者直接通过，否则检查分享权限。</p>
 */
public interface FilePermissionService {

    /**
     * 检查用户是否有权查看指定文件。
     *
     * @param userId     用户 ID
     * @param userFileId 用户文件 ID
     * @return 有查看权限返回 true
     */
    boolean canView(Long userId, Long userFileId);

    /**
     * 检查用户是否有权编辑指定文件。
     *
     * @param userId     用户 ID
     * @param userFileId 用户文件 ID
     * @return 有编辑权限返回 true
     */
    boolean canEdit(Long userId, Long userFileId);
}
