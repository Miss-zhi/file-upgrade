package com.qiwenshare.auth.event;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件。
 *
 * <p>当新用户注册成功后发布，file 模块监听此事件创建初始配额记录。</p>
 */
public class UserRegisteredEvent extends ApplicationEvent {

    private final String userId;

    /**
     * 构造用户注册事件。
     *
     * @param source 事件源
     * @param userId 新注册用户的业务 ID（Snowflake 字符串）
     */
    public UserRegisteredEvent(Object source, String userId) {
        super(source);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
