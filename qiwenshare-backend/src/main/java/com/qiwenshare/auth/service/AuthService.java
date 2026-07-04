package com.qiwenshare.auth.service;

import com.qiwenshare.auth.common.CookieUtils;
import com.qiwenshare.auth.common.SnowflakeIdGenerator;
import com.qiwenshare.auth.config.AuthProperties;
import com.qiwenshare.auth.config.JwtProperties;
import com.qiwenshare.auth.dto.ChangePasswordRequest;
import com.qiwenshare.auth.dto.LoginRequest;
import com.qiwenshare.auth.dto.RegisterRequest;
import com.qiwenshare.auth.dto.ResetPasswordRequest;
import com.qiwenshare.auth.entity.Permission;
import com.qiwenshare.auth.entity.Role;
import com.qiwenshare.auth.entity.RolePermission;
import com.qiwenshare.auth.entity.User;
import com.qiwenshare.auth.entity.UserRole;
import com.qiwenshare.auth.event.PermissionChangedEvent;
import com.qiwenshare.auth.exception.AuthErrorCode;
import com.qiwenshare.auth.exception.AuthException;
import com.qiwenshare.auth.repository.*;
import com.qiwenshare.auth.vo.LoginResponse;
import com.qiwenshare.auth.vo.RoleResponse;
import com.qiwenshare.auth.vo.UserInfoResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证业务逻辑服务。
 *
 * <p>包含注册、登录、登出、刷新、修改密码、重置密码等核心业务方法。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,30}$";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CookieUtils cookieUtils;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 新用户的 userId
     */
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 校验唯一性
        if (userRepository.existsByUsername(request.username())) {
            throw new AuthException(AuthErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.existsByTelephone(request.telephone())) {
            throw new AuthException(AuthErrorCode.TELEPHONE_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUserId(snowflakeIdGenerator.generate());
        user.setUsername(request.username());
        user.setTelephone(request.telephone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRegisterTime(LocalDateTime.now());
        user.setAvailable(1);
        userRepository.save(user);

        // 绑定默认角色（roleId=2，普通用户）
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2);
        userRoleRepository.save(userRole);

        log.info("用户注册成功: userId={}, username={}", user.getUserId(), user.getUsername());

        // 发布用户注册事件（file 模块监听创建初始配额）
        eventPublisher.publishEvent(new com.qiwenshare.auth.event.UserRegisteredEvent(this, user.getUserId()));

        return user.getUserId();
    }

    /**
     * 用户登录。
     *
     * @param request  登录请求
     * @param response HTTP 响应（用于设置 cookie）
     * @return 登录响应
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        String telephone = request.telephone();

        // 检查登录失败计数
        String failKey = LOGIN_FAIL_PREFIX + telephone;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= authProperties.loginFailMax()) {
            throw new AuthException(AuthErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        // 查询用户
        User user = userRepository.findByTelephone(telephone)
                .orElse(null);
        if (user == null) {
            incrementLoginFail(telephone);
            throw new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 检查账户状态
        if (user.getAvailable() == 0) {
            throw new AuthException(AuthErrorCode.AUTH_USER_DISABLED);
        }

        // 验证密码（含 MD5 透明迁移）
        if (!verifyPassword(request.password(), user)) {
            incrementLoginFail(telephone);
            throw new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        // 加载角色和权限（批量查询，避免 N+1）
        RolesAndPerms rap = loadRolesAndPerms(user.getId());
        List<String> roles = rap.roleNames;
        List<String> permissions = rap.permKeys;

        // 生成 token pair
        String accessToken = tokenService.generateAccessToken(user.getUserId(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getUserId());

        // 设置 cookie
        setTokenCookies(response, accessToken, refreshToken);

        log.info("用户登录成功: userId={}", user.getUserId());
        return new LoginResponse(user.getUserId(), user.getUsername(), roles, permissions);
    }

    /**
     * Token 刷新。
     *
     * @param request  HTTP 请求（从 cookie 提取 refresh token）
     * @param response HTTP 响应（设置新 cookie）
     */
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refresh_token");
        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }

        Claims claims = tokenService.parseAndValidate(refreshToken);
        if (claims == null) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }

        String jti = claims.get("jti", String.class);
        String userId = claims.getSubject();

        // 消费 refresh token（rotation）
        String storedUserId = tokenService.consumeRefreshToken(jti);
        if (storedUserId == null) {
            // 重用检测触发
            log.warn("Refresh token 重用检测: userId={}, jti={}", userId, jti);
            tokenService.revokeAllRefreshTokens(userId);
            tokenService.revokeAllTokens(userId);
            throw new AuthException(AuthErrorCode.TOKEN_REUSE_DETECTED);
        }

        // 加载角色
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        List<String> roles = loadRoleNames(user.getId());

        // 生成新 token pair
        String newAccessToken = tokenService.generateAccessToken(userId, roles);
        String newRefreshToken = tokenService.generateRefreshToken(userId);

        setTokenCookies(response, newAccessToken, newRefreshToken);
        log.info("Token 刷新成功: userId={}", userId);
    }

    /**
     * 用户登出（幂等操作）。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 黑名单 access token
        String accessToken = extractCookieValue(request, "access_token");
        if (accessToken != null) {
            Claims accessClaims = tokenService.parseAndValidate(accessToken);
            if (accessClaims != null) {
                String jti = accessClaims.get("jti", String.class);
                Instant exp = accessClaims.getExpiration().toInstant();
                long remaining = tokenService.remainingSeconds(exp);
                tokenService.blacklist(jti, remaining);
            }
        }

        // 消费并黑名单 refresh token
        String refreshToken = extractCookieValue(request, "refresh_token");
        if (refreshToken != null) {
            Claims refreshClaims = tokenService.parseAndValidate(refreshToken);
            if (refreshClaims != null) {
                String jti = refreshClaims.get("jti", String.class);
                if (jti != null) {
                    tokenService.consumeRefreshToken(jti);
                    Instant exp = refreshClaims.getExpiration().toInstant();
                    long remaining = tokenService.remainingSeconds(exp);
                    tokenService.blacklist(jti, remaining);
                }
            }
        }

        // 清除 cookie
        clearTokenCookies(response);
        log.info("用户登出成功");
    }

    /**
     * 获取当前用户信息。
     *
     * @return 用户信息响应
     */
    public UserInfoResponse getCurrentUser() {
        String userId = getCurrentUserId();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getAvailable() == 0) {
            throw new AuthException(AuthErrorCode.AUTH_USER_DISABLED);
        }

        RolesAndPerms rap = loadRolesAndPerms(user.getId());

        // 手机号脱敏
        String maskedPhone = maskTelephone(user.getTelephone());

        return new UserInfoResponse(
                user.getUserId(),
                user.getUsername(),
                maskedPhone,
                user.getAvatar(),
                rap.roleNames,
                rap.permKeys,
                user.getRegisterTime()
        );
    }

    /**
     * 修改密码。
     *
     * @param request 修改密码请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(ChangePasswordRequest request) {
        String userId = getCurrentUserId();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 验证旧密码
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.AUTH_OLD_PASSWORD_WRONG);
        }

        // 校验新密码强度
        validatePasswordStrength(request.newPassword());

        // 校验新旧密码不同
        if (request.oldPassword().equals(request.newPassword())) {
            throw new AuthException(AuthErrorCode.PASSWORD_SAME);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // 全局撤销 token
        tokenService.revokeAllTokens(userId);

        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 管理员重置密码。
     *
     * @param targetUserId 目标用户 ID
     * @param request      重置密码请求
     * @param operatorId   操作人 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String targetUserId, ResetPasswordRequest request, String operatorId) {
        User user = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        validatePasswordStrength(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // 全局撤销 token
        tokenService.revokeAllTokens(targetUserId);

        log.info("管理员重置密码: operator={}, target={}", operatorId, targetUserId);
    }

    /**
     * 查询所有角色及其权限列表。
     *
     * @return 角色响应列表
     */
    public List<RoleResponse> listRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(role -> {
            List<Integer> permIds = rolePermissionRepository.findByRoleId(role.getRoleId())
                    .stream()
                    .map(RolePermission::getPermissionId)
                    .toList();
            return new RoleResponse(
                    role.getRoleId(),
                    role.getRoleName(),
                    role.getRoleDesc(),
                    role.getAvailable(),
                    permIds
            );
        }).toList();
    }

    /**
     * 更新角色权限（全量替换）。
     *
     * @param roleId      角色 ID
     * @param permissionIds 新的权限 ID 列表
     * @return 受影响的用户业务 ID 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> updateRolePermissions(Integer roleId, List<Integer> permissionIds) {
        // 删除旧绑定
        rolePermissionRepository.deleteByRoleId(roleId);

        // 创建新绑定
        for (Integer permId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionRepository.save(rp);
        }

        // 查找受影响的用户
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        List<String> affectedUserIds = new ArrayList<>();
        for (UserRole ur : userRoles) {
            User user = userRepository.findById(ur.getUserId()).orElse(null);
            if (user != null) {
                affectedUserIds.add(user.getUserId());
            }
        }

        log.info("角色权限更新: roleId={}, permissionCount={}, affectedUsers={}",
                roleId, permissionIds.size(), affectedUserIds.size());
        return affectedUserIds;
    }

    // ===== 私有辅助方法 =====

    private boolean verifyPassword(String rawPassword, User user) {
        // 1. 先尝试 BCrypt
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return true;
        }
        // 2. 尝试旧 MD5（仅当 salt 有值时）
        if (user.getSalt() != null && user.getOldPassword() != null) {
            String md5Hash = hashMd5(rawPassword, user.getSalt());
            if (md5Hash.equals(user.getPassword())) {
                // 透明迁移：乐观锁更新
                String bcryptHash = passwordEncoder.encode(rawPassword);
                int updated = userRepository.migratePassword(bcryptHash, user.getUserId(), user.getPassword());
                if (updated > 0) {
                    log.info("MD5→BCrypt 迁移成功: userId={}", user.getUserId());
                }
                return true;
            }
        }
        return false;
    }

    private String hashMd5(String password, String salt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            String input = password + salt;
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void incrementLoginFail(String telephone) {
        String key = LOGIN_FAIL_PREFIX + telephone;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(authProperties.lockDurationMinutes()));
        }
    }

    /**
     * 批量加载用户角色和权限（3 条 SQL 代替原来的 N+M 条）。
     */
    private RolesAndPerms loadRolesAndPerms(Long userPkId) {
        // 1. 查用户角色绑定
        List<UserRole> userRoles = userRoleRepository.findByUserId(userPkId);
        List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();

        if (roleIds.isEmpty()) {
            return new RolesAndPerms(List.of(), List.of());
        }

        // 2. 批量查可用角色名（1 条 SQL）
        Map<Integer, Role> roleMap = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getAvailable() == 1)
                .collect(Collectors.toMap(Role::getRoleId, r -> r));

        List<String> roleNames = roleIds.stream()
                .filter(roleMap::containsKey)
                .map(id -> roleMap.get(id).getRoleName())
                .toList();

        // 3. 批量查角色权限 + 权限详情（2 条 SQL）
        List<Integer> availableRoleIds = roleIds.stream()
                .filter(roleMap::containsKey)
                .toList();

        Set<String> permKeys = new HashSet<>();
        if (!availableRoleIds.isEmpty()) {
            List<RolePermission> rps = rolePermissionRepository.findByRoleIdIn(availableRoleIds);
            Set<Integer> permIds = rps.stream()
                    .map(RolePermission::getPermissionId)
                    .collect(Collectors.toSet());
            if (!permIds.isEmpty()) {
                permissionRepository.findAllById(permIds)
                        .forEach(p -> permKeys.add(p.getPermKey()));
            }
        }

        return new RolesAndPerms(roleNames, new ArrayList<>(permKeys));
    }

    /**
     * 仅加载角色名列表（用于 token 刷新，只需角色名）。
     */
    private List<String> loadRoleNames(Long userPkId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userPkId);
        if (userRoles.isEmpty()) return List.of();
        List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        return roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getAvailable() == 1)
                .map(Role::getRoleName)
                .toList();
    }

    /** 角色+权限的批量加载结果。 */
    private record RolesAndPerms(List<String> roleNames, List<String> permKeys) {}

    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = cookieUtils.buildCookie(
                "access_token", accessToken, jwtProperties.accessTokenTtl(), "/");
        ResponseCookie refreshCookie = cookieUtils.buildCookie(
                "refresh_token", refreshToken, jwtProperties.refreshTokenTtl(), "/api/v1/auth/refresh");
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookieUtils.clearCookie("access_token", "/").toString());
        response.addHeader("Set-Cookie", cookieUtils.clearCookie("refresh_token", "/api/v1/auth/refresh").toString());
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new AuthException(AuthErrorCode.AUTH_NOT_AUTHENTICATED);
    }

    private void validatePasswordStrength(String password) {
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new AuthException(AuthErrorCode.PASSWORD_WEAK);
        }
    }

    private String maskTelephone(String telephone) {
        if (telephone == null || telephone.length() < 7) return telephone;
        return telephone.substring(0, 3) + "****" + telephone.substring(7);
    }
}
