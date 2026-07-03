package com.qiwenshare.auth.service;

import com.qiwenshare.auth.dto.UserListQuery;
import com.qiwenshare.auth.entity.Permission;
import com.qiwenshare.auth.entity.Role;
import com.qiwenshare.auth.entity.RolePermission;
import com.qiwenshare.auth.entity.User;
import com.qiwenshare.auth.entity.UserRole;
import com.qiwenshare.auth.event.PermissionChangeEventPublisher;
import com.qiwenshare.auth.exception.AuthErrorCode;
import com.qiwenshare.auth.exception.AuthException;
import com.qiwenshare.auth.repository.PermissionRepository;
import com.qiwenshare.auth.repository.RolePermissionRepository;
import com.qiwenshare.auth.repository.RoleRepository;
import com.qiwenshare.auth.repository.UserRepository;
import com.qiwenshare.auth.repository.UserRoleRepository;
import com.qiwenshare.auth.vo.UserDetailVO;
import com.qiwenshare.auth.vo.UserListVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务。
 *
 * <p>提供用户列表查询、详情查询、启用/禁用等业务逻辑。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionChangeEventPublisher publisher;

    /**
     * 分页查询用户列表。
     * 使用批量查询优化 N+1 问题：一次性加载所有用户的角色关联和角色信息。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    public Page<UserListVO> listUsers(UserListQuery query) {
        Specification<User> spec = buildUserSpec(query);
        PageRequest pageRequest = PageRequest.of(query.page(), query.pageSize(), Sort.by("id").descending());
        Page<User> page = userRepository.findAll(spec, pageRequest);

        if (page.isEmpty()) {
            return page.map(user -> new UserListVO(
                    user.getUserId(), user.getUsername(), user.getTelephone(),
                    user.getAvailable(), user.getRegisterTime(), List.of()));
        }

        // 批量查询所有用户角色关联（1 次查询代替 N 次）
        List<Long> userPkIds = page.getContent().stream().map(User::getId).toList();
        List<UserRole> allUserRoles = userRoleRepository.findByUserIdIn(userPkIds);

        // 批量查询所有角色（1 次查询代替 N 次）
        Set<Integer> roleIds = allUserRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Map<Integer, Role> roleMap = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getAvailable() == 1)
                .collect(Collectors.toMap(Role::getRoleId, r -> r));

        // 按用户分组构建角色名称列表
        Map<Long, List<String>> userRoleMap = new HashMap<>();
        for (UserRole ur : allUserRoles) {
            Role role = roleMap.get(ur.getRoleId());
            if (role != null) {
                userRoleMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>())
                        .add(role.getRoleName());
            }
        }

        return page.map(user -> new UserListVO(
                user.getUserId(),
                user.getUsername(),
                user.getTelephone(),
                user.getAvailable(),
                user.getRegisterTime(),
                userRoleMap.getOrDefault(user.getId(), List.of())
        ));
    }

    /**
     * 查询用户详情（含角色和权限列表）。
     * 使用批量查询优化：一次性加载角色和权限，避免 N+1 问题。
     *
     * @param userId 用户业务 ID
     * @return 用户详情
     */
    public UserDetailVO getUserDetail(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());

        // 批量查询所有关联角色（1 次查询代替 N 次）
        Set<Integer> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Map<Integer, Role> roleMap = roleIds.isEmpty()
                ? Map.of()
                : roleRepository.findAllById(roleIds).stream()
                    .filter(r -> r.getAvailable() == 1)
                    .collect(Collectors.toMap(Role::getRoleId, r -> r));

        List<UserDetailVO.RoleInfo> roles = userRoles.stream()
                .map(ur -> roleMap.get(ur.getRoleId()))
                .filter(role -> role != null)
                .map(role -> new UserDetailVO.RoleInfo(role.getRoleId(), role.getRoleName()))
                .toList();

        // 批量查询所有权限（1 次查询代替 N*M 次）
        List<RolePermission> allRolePerms = roleIds.isEmpty()
                ? List.of()
                : rolePermissionRepository.findByRoleIdIn(roleIds);
        Set<Integer> permIds = allRolePerms.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
        Set<String> permKeys = permIds.isEmpty()
                ? Set.of()
                : permissionRepository.findAllById(permIds).stream()
                    .map(p -> p.getPermKey())
                    .collect(Collectors.toSet());

        return new UserDetailVO(
                user.getUserId(),
                user.getUsername(),
                user.getTelephone(),
                user.getAvailable(),
                user.getRegisterTime(),
                roles,
                new ArrayList<>(permKeys)
        );
    }

    /**
     * 启用用户。
     *
     * @param userId 用户业务 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getAvailable() == 1) {
            return; // 幂等
        }

        user.setAvailable(1);
        userRepository.save(user);

        publisher.publishPermissionChanged(List.of(userId));
        log.info("管理员启用用户: userId={}", userId);
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户业务 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(String userId) {
        // 不能禁用自己
        String currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new AuthException(AuthErrorCode.CANNOT_DISABLE_SELF);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getAvailable() == 0) {
            return; // 幂等
        }

        user.setAvailable(0);
        userRepository.save(user);

        publisher.publishPermissionChanged(List.of(userId));
        log.info("管理员禁用用户: userId={}", userId);
    }

    private Specification<User> buildUserSpec(UserListQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.keyword() != null && !query.keyword().isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + query.keyword() + "%"));
            }
            if (query.available() != null) {
                predicates.add(cb.equal(root.get("available"), query.available()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new AuthException(AuthErrorCode.AUTH_NOT_AUTHENTICATED);
    }
}
