package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.config.jwt.JwtUtil;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public String login(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            throw new QiwenException(400, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new QiwenException(400, "用户名或密码错误");
        }
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    @Override
    @Transactional
    public void register(String username, String password, String email) {
        // 检查用户名唯一性
        User existUser = findByUsername(username);
        if (existUser != null) {
            throw new QiwenException(400, "用户名已存在");
        }

        User user = new User();
        user.setId(IdUtil.getSnowflakeNextIdStr());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(username);
        user.setStatus(1);
        user.setRole("USER");  // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
    }

    @Override
    public User getUserById(String userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public IPage<User> listUsers(Integer page, Integer size, String keyword) {
        Page<User> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional
    public void updateUser(String id, String email, String phone, String nickname, String avatar) {
        User user = userMapper.selectById(id);
        if (user == null) throw new QiwenException(404, "用户不存在");
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(String id, boolean enabled) {
        User user = userMapper.selectById(id);
        if (user == null) throw new QiwenException(404, "用户不存在");
        user.setStatus(enabled ? 1 : 0);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateRole(String userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new QiwenException(404, "用户不存在");
        if (!"ADMIN".equals(role) && !"USER".equals(role))
            throw new QiwenException(400, "无效角色，仅支持 ADMIN 或 USER");
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
