package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.domain.user.User;

/**
 * 用户服务接口
 */
public interface IUserService {

    User findByUsername(String username);

    String login(String username, String password);

    void register(String username, String password, String email);

    User getUserById(String userId);

    /** 分页搜索用户 */
    IPage<User> listUsers(Integer page, Integer size, String keyword);

    /** 更新用户信息 */
    void updateUser(String id, String email, String phone, String nickname, String avatar);

    /** 删除用户 */
    void deleteUser(String id);

    /** 切换用户启用/禁用状态 */
    void toggleStatus(String id, boolean enabled);

    /** 更新用户角色 */
    void updateRole(String userId, String role);
}
