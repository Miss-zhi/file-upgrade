package com.qiwenshare.auth.repository;

import com.qiwenshare.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据手机号查询用户。
     *
     * @param telephone 手机号
     * @return 用户实体
     */
    Optional<User> findByTelephone(String telephone);

    /**
     * 根据业务 ID 查询用户。
     *
     * @param userId Snowflake 业务 ID
     * @return 用户实体
     */
    Optional<User> findByUserId(String userId);

    /**
     * 检查手机号是否已存在。
     *
     * @param telephone 手机号
     * @return 是否存在
     */
    boolean existsByTelephone(String telephone);

    /**
     * 检查用户名是否已存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 乐观锁更新密码（用于 MD5→BCrypt 透明迁移）。
     * 仅当当前密码值与 {@code oldPasswordHash} 匹配时才更新。
     *
     * @param newBcryptHash 新的 BCrypt hash
     * @param userId        用户业务 ID
     * @param oldPasswordHash 旧密码 hash（WHERE 条件）
     * @return 受影响行数（0 表示已被其他线程迁移）
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :newBcryptHash, u.oldPassword = null, u.salt = null " +
           "WHERE u.userId = :userId AND u.password = :oldPasswordHash")
    int migratePassword(@Param("newBcryptHash") String newBcryptHash,
                        @Param("userId") String userId,
                        @Param("oldPasswordHash") String oldPasswordHash);
}
