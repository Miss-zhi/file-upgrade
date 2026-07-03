package com.qiwenshare.file.listener;

import com.qiwenshare.auth.event.UserRegisteredEvent;
import com.qiwenshare.file.entity.StorageBean;
import com.qiwenshare.file.repository.StorageBeanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户注册事件监听器。
 *
 * <p>监听 {@link UserRegisteredEvent}，为新注册用户创建初始存储配额记录。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final StorageBeanRepository storageBeanRepository;

    /**
     * 处理用户注册事件，创建初始配额记录。
     *
     * @param event 用户注册事件
     */
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            Long userId = Long.parseLong(event.getUserId());
            StorageBean storageBean = new StorageBean();
            storageBean.setUserId(userId);
            // totalQuota 默认 10GB（Entity 中已设置默认值）
            storageBeanRepository.save(storageBean);
            log.info("为新用户创建初始配额: userId={}", userId);
        } catch (Exception e) {
            log.error("创建用户初始配额失败: userId={}", event.getUserId(), e);
        }
    }
}
