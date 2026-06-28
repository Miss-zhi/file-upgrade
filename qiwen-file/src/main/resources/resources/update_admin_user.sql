-- ================================================================================
-- 管理员账户初始化脚本 (Docker 容器化部署专用)
-- ================================================================================
-- 说明：
-- 1. 本脚本在 MySQL 容器首次初始化时自动执行，无需手动运行
-- 2. 默认管理员账户：16727137637，密码：123456
-- 3. 手机号符合前端登录页校验规则（11位，格式 /^1[3-9]\d{9}$/）
-- 4. 密码加密方式：MD5(password + salt, 1024 iterations)
-- ================================================================================

-- 等待 JPA 自动建表完成（重要：确保表结构已存在）
-- MySQL 的 docker-entrypoint-initdb.d 机制会在数据库初始化时执行此脚本

-- 插入管理员账户（如果不存在）
-- Salt: 'qiwen_admin_salt'
-- 原始密码: '123456'
-- 加密后的密码: MD5('123456' + 'qiwen_admin_salt', 1024次迭代)
INSERT INTO user (username, telephone, salt, password, registerTime, available) 
SELECT 
    'admin',
    '16727137637',
    'qiwen_admin_salt',
    'e10adc3949ba59abbe56e057f20f883e',  -- MD5加密后的密码
    NOW(),
    1
WHERE NOT EXISTS (SELECT 1 FROM user WHERE telephone = '16727137637');

-- 为管理员分配超级管理员角色（roleId=1）
INSERT INTO user_role (userId, roleId) 
SELECT u.userId, 1 
FROM user u 
WHERE u.telephone = '16727137637' 
AND NOT EXISTS (
    SELECT 1 FROM user_role ur 
    WHERE ur.userId = u.userId AND ur.roleId = 1
);

-- 初始化管理员的存储配额（1GB = 1073741824 bytes）
INSERT INTO storage (userId, totalStorageSize, storageSize) 
SELECT u.userId, 1073741824, 0
FROM user u 
WHERE u.telephone = '16727137637'
AND NOT EXISTS (
    SELECT 1 FROM storage s WHERE s.userId = u.userId
);

-- ================================================================================
-- 验证查询（仅用于调试，生产环境可注释）
-- ================================================================================
SELECT 
    u.userId AS '用户ID', 
    u.username AS '用户名', 
    u.telephone AS '手机号', 
    r.roleName AS '角色',
    s.totalStorageSize AS '总存储空间(字节)',
    u.available AS '账户状态',
    u.registerTime AS '注册时间'
FROM user u
LEFT JOIN user_role ur ON u.userId = ur.userId
LEFT JOIN role r ON ur.roleId = r.roleId
LEFT JOIN storage s ON u.userId = s.userId
WHERE u.telephone = '16727137637';

-- ================================================================================
-- 重要提示：
-- 1. 如需修改默认密码，请使用以下 Java 代码生成新的加密密码：
--    String newSalt = "your_custom_salt";
--    String newPassword = HashUtils.hashHex("MD5", "your_password", newSalt, 1024);
-- 2. 生产环境请务必修改默认密码和 Salt 值
-- 3. 首次登录后请立即通过系统修改密码
-- ================================================================================
