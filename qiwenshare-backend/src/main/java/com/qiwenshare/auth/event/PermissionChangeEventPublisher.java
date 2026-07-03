package com.qiwenshare.auth.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限变更事件发布器。
 *
 * <p>在角色或权限变更时调用 {@link #publishPermissionChanged(List)} 发布事件。</p>
 */
@Component
@RequiredArgsConstructor
public class PermissionChangeEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布权限变更事件。
     *
     * @param userIds 受影响的用户业务 ID 列表
     */
    public void publishPermissionChanged(List<String> userIds) {
        eventPublisher.publishEvent(new PermissionChangedEvent(this, userIds));
    }
}
