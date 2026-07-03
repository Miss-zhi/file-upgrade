package com.qiwenshare.file.event;

import org.springframework.context.ApplicationEvent;

/**
 * 文件变更事件。
 *
 * <p>当文件创建、更新或删除时发布，搜索模块监听此事件异步更新 ES 索引。
 * file 模块发布此事件，search 模块消费。</p>
 */
public class FileChangedEvent extends ApplicationEvent {

    /**
     * 变更类型枚举。
     */
    public enum ChangeType {
        /** 文件创建 */
        CREATED,
        /** 文件更新（重命名/移动） */
        UPDATED,
        /** 文件删除 */
        DELETED
    }

    private final Long userFileId;
    private final ChangeType changeType;

    /**
     * 构造文件变更事件。
     *
     * @param source      事件源
     * @param userFileId  用户文件 ID
     * @param changeType  变更类型
     */
    public FileChangedEvent(Object source, Long userFileId, ChangeType changeType) {
        super(source);
        this.userFileId = userFileId;
        this.changeType = changeType;
    }

    public Long getUserFileId() {
        return userFileId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
