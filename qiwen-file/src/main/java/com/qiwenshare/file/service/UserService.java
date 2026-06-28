package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        return jwtUtil.generateToken(user.getId());
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
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
    }

    @Override
    public User getUserById(String userId) {
        return userMapper.selectById(userId);
    }
}
