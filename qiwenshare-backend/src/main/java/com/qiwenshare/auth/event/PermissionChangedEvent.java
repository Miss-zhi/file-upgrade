package com.qiwenshare.auth.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 权限变更事件。
 *
 * <p>当角色或权限发生变更时发布，监听器负责删除相关用户的 Redis 权限缓存。</p>
 */
public class PermissionChangedEvent extends ApplicationEvent {

    private final List<String> userIds;

    /**
     * 构造权限变更事件。
     *
     * @param source   事件源
     * @param userIds  受影响的用户业务 ID 列表
     */
    public PermissionChangedEvent(Object source, List<String> userIds) {
        super(source);
        this.userIds = userIds;
    }

    public List<String> getUserIds() {
        return userIds;
    }
}
