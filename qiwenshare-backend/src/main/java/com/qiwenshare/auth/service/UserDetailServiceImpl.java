package com.qiwenshare.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.entity.Permission;
import com.qiwenshare.auth.entity.Role;
import com.qiwenshare.auth.entity.RolePermission;
import com.qiwenshare.auth.entity.User;
import com.qiwenshare.auth.entity.UserRole;
import com.qiwenshare.auth.repository.PermissionRepository;
import com.qiwenshare.auth.repository.RolePermissionRepository;
import com.qiwenshare.auth.repository.RoleRepository;
import com.qiwenshare.auth.repository.UserRepository;
import com.qiwenshare.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link UserDetailsService} 实现。
 *
 * <p>根据 userId（Snowflake 业务 ID）加载用户信息和权限。
 * 优先从 Redis 缓存读取权限（key: {@code user:perms:{userId}}，TTL: 5 分钟），
 * 缓存 miss 时查数据库并回填。</p>
 *
 * <p>角色名称在构建 {@link GrantedAuthority} 时加 {@code ROLE_} 前缀，
 * 权限编码不加前缀。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    private static final String PERM_CACHE_PREFIX = "user:perms:";
    private static final Duration PERM_CACHE_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 根据用户业务 ID 加载用户详情。
     *
     * @param userId Snowflake 业务 ID
     * @return UserDetails 实例
     * @throws UsernameNotFoundException 用户不存在或已禁用
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userId));

        if (user.getAvailable() == 0) {
            throw new UsernameNotFoundException("用户已禁用: " + userId);
        }

        List<GrantedAuthority> authorities = loadAuthorities(user.getId(), userId);

        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                user.getAvailable() == 1,
                true, true, true,
                authorities
        );
    }

    /**
     * 加载用户权限列表（角色加 ROLE_ 前缀，权限不加）。
     */
    private List<GrantedAuthority> loadAuthorities(Long userPkId, String userId) {
        // 先尝试从 Redis 缓存读取
        List<String> permKeys = loadPermKeysFromCache(userId);
        if (permKeys != null) {
            return buildAuthorities(userPkId, permKeys);
        }

        // 缓存 miss，从数据库加载
        permKeys = loadPermKeysFromDb(userPkId);

        // 回填缓存
        cachePermKeys(userId, permKeys);

        return buildAuthorities(userPkId, permKeys);
    }

    private List<String> loadPermKeysFromCache(String userId) {
        try {
            String cached = redisTemplate.opsForValue().get(PERM_CACHE_PREFIX + userId);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.debug("读取权限缓存失败: {}", e.getMessage());
        }
        return null;
    }

    private List<String> loadPermKeysFromDb(Long userPkId) {
        // 查用户角色绑定
        List<UserRole> userRoles = userRoleRepository.findByUserId(userPkId);
        List<Integer> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .toList();

        // 查可用角色
        Set<String> roleNames = new HashSet<>();
        for (Integer roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null && role.getAvailable() == 1) {
                roleNames.add(role.getRoleName());
            }
        }

        // 查角色权限
        Set<String> permKeys = new HashSet<>();
        for (Integer roleId : roleIds) {
            List<RolePermission> rolePerms = rolePermissionRepository.findByRoleId(roleId);
            for (RolePermission rp : rolePerms) {
                Permission perm = permissionRepository.findById(rp.getPermissionId()).orElse(null);
                if (perm != null) {
                    permKeys.add(perm.getPermKey());
                }
            }
        }

        // 合并角色名和权限编码
        List<String> result = new ArrayList<>();
        for (String roleName : roleNames) {
            result.add("ROLE_" + roleName);
        }
        result.addAll(permKeys);
        return result;
    }

    private void cachePermKeys(String userId, List<String> permKeys) {
        try {
            String json = objectMapper.writeValueAsString(permKeys);
            redisTemplate.opsForValue().set(PERM_CACHE_PREFIX + userId, json, PERM_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.debug("缓存权限失败: {}", e.getMessage());
        }
    }

    private List<GrantedAuthority> buildAuthorities(Long userPkId, List<String> permKeys) {
        return permKeys.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
