package com.qiwenshare.file.vo;

import java.util.List;

/**
 * 批量操作结果响应。
 *
 * @param successCount 成功数量
 * @param failedItems  失败项列表
 */
public record BatchOperationResultVO(
        int successCount,
        List<FailedItem> failedItems
) {

    /**
     * 失败项详情。
     *
     * @param userFileId 文件 ID
     * @param reason     失败原因
     */
    public record FailedItem(Long userFileId, String reason) {}
}
