package com.qiwenshare.file.api;

import com.qiwenshare.file.domain.user.User;

/**
 * 用户服务接口
 */
public interface IUserService {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 用户登录，验证成功后返回 JWT Token
     */
    String login(String username, String password);

    /**
     * 用户注册
     */
    void register(String username, String password, String email);

    /**
     * 根据 ID 获取用户信息
     */
    User getUserById(String userId);
}
