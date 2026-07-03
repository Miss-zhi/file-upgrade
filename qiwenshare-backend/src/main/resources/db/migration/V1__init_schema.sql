-- ============================================================
-- 奇文网盘 - 数据库初始化脚本（全量）
-- 整合所有模块建表语句与初始数据
-- ============================================================

-- ============================================================
-- 模块一：auth（认证授权）
-- ============================================================

CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`       VARCHAR(32)  NOT NULL COMMENT 'Snowflake 业务 ID',
  `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
  `telephone`     VARCHAR(20)  NOT NULL COMMENT '手机号',
  `password`      VARCHAR(100) NOT NULL COMMENT 'BCrypt hash',
  `old_password`  VARCHAR(64)  DEFAULT NULL COMMENT 'MD5 旧密码（迁移完成后清空）',
  `salt`          VARCHAR(64)  DEFAULT NULL COMMENT '旧 MD5 salt（迁移完成后清空）',
  `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `register_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `available`     INT       NOT NULL DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_telephone` (`telephone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `role` (
  `role_id`   INT          NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `role_name` VARCHAR(30)  NOT NULL COMMENT '角色名称（不含 ROLE_ 前缀）',
  `role_desc` VARCHAR(100) DEFAULT NULL COMMENT '角色描述',
  `available` INT       NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `permission` (
  `permission_id` INT        NOT NULL AUTO_INCREMENT COMMENT '权限 ID',
  `perm_key`      VARCHAR(50) NOT NULL COMMENT '权限编码 resource:action',
  `perm_name`     VARCHAR(50) NOT NULL COMMENT '权限名称',
  `parent_id`     INT        NOT NULL DEFAULT 0 COMMENT '父权限 ID，0 表示顶级',
  `perm_type`     INT        NOT NULL DEFAULT 1 COMMENT '1-菜单 2-按钮 3-API',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_perm_key` (`perm_key`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` BIGINT NOT NULL COMMENT 'user.id',
  `role_id` INT    NOT NULL COMMENT 'role.role_id',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `role_permission` (
  `role_id`       INT NOT NULL COMMENT 'role.role_id',
  `permission_id` INT NOT NULL COMMENT 'permission.permission_id',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 初始角色数据
INSERT INTO `role` (`role_id`, `role_name`, `role_desc`, `available`) VALUES
  (1, 'ADMIN', '系统管理员', 1),
  (2, 'USER', '普通用户', 1);

-- 初始权限数据
INSERT INTO `permission` (`perm_key`, `perm_name`, `parent_id`, `perm_type`) VALUES
  ('admin:user-manage', '用户管理', 0, 2),
  ('admin:role-manage', '角色管理', 0, 2),
  ('admin:quota-manage', '配额管理', 0, 3),
  ('admin:log-view', '日志查看', 0, 3),
  ('admin:config-manage', '系统配置管理', 0, 3),
  ('file:upload', '文件上传', 0, 3),
  ('file:download', '文件下载', 0, 3),
  ('file:delete', '文件删除', 0, 3),
  ('file:move', '文件移动', 0, 3),
  ('file:rename', '文件重命名', 0, 3),
  ('file:share', '文件分享', 0, 3),
  ('file:recycle', '回收站操作', 0, 3);

-- ADMIN 拥有所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `permission_id` FROM `permission`;

-- USER 拥有文件操作权限（不含 admin:*）
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, `permission_id` FROM `permission` WHERE `perm_key` NOT LIKE 'admin:%';

-- ============================================================
-- 模块二：file（文件管理）
-- ============================================================

CREATE TABLE IF NOT EXISTS `file_bean` (
  `file_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `file_size`     BIGINT       NOT NULL COMMENT '文件大小（字节）',
  `file_hash`     VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hash',
  `storage_type`  VARCHAR(20)  NOT NULL COMMENT '存储后端类型 local/minio/aliyun/qiniu/fastdfs',
  `storage_path`  VARCHAR(500) NOT NULL COMMENT '存储后端中的物理路径',
  `file_status`   INT       NOT NULL DEFAULT 1 COMMENT '1-正常 0-已清理',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  UNIQUE KEY `uk_file_hash_size` (`file_hash`, `file_size`),
  KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物理文件元数据';

CREATE TABLE IF NOT EXISTS `user_file` (
  `user_file_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`          BIGINT       NOT NULL COMMENT '用户 ID（关联 user.id）',
  `file_id`          BIGINT       DEFAULT NULL COMMENT '关联 file_bean.file_id（文件夹为 NULL）',
  `file_name`        VARCHAR(255) NOT NULL COMMENT '文件名（不含扩展名）',
  `extend_name`      VARCHAR(20)  DEFAULT '' COMMENT '文件扩展名（不含点号）',
  `file_path`        VARCHAR(255) NOT NULL DEFAULT '/' COMMENT '虚拟目录路径（/分隔）',
  `file_type`        INT          NOT NULL DEFAULT 1 COMMENT '1-普通文件 2-文件夹',
  `delete_status`    INT          NOT NULL DEFAULT 0 COMMENT '0-正常 1-已删除',
  `delete_time`      DATETIME     DEFAULT NULL COMMENT '删除时间',
  `delete_batch_num` VARCHAR(32)  DEFAULT NULL COMMENT '删除批次号',
  `upload_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_file_id`),
  UNIQUE KEY `uk_user_path_name` (`user_id`, `file_path`, `file_name`, `extend_name`, `delete_status`, `file_type`),
  KEY `idx_user_id_path` (`user_id`, `file_path`),
  KEY `idx_delete_status` (`user_id`, `delete_status`, `delete_time`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户文件关联';

CREATE TABLE IF NOT EXISTS `upload_task` (
  `task_id`         VARCHAR(64)  NOT NULL COMMENT '上传任务 ID',
  `user_id`         BIGINT       NOT NULL,
  `file_name`       VARCHAR(255) NOT NULL,
  `file_hash`       VARCHAR(64)  NOT NULL,
  `file_size`       BIGINT       NOT NULL COMMENT '文件总大小',
  `total_chunks`    INT          NOT NULL COMMENT '总分片数',
  `uploaded_chunks` INT          NOT NULL DEFAULT 0 COMMENT '已上传分片数',
  `status`          INT          NOT NULL DEFAULT 0 COMMENT '0-进行中 1-合并中 2-完成 3-失败',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分片上传任务';

CREATE TABLE IF NOT EXISTS `upload_task_detail` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`       VARCHAR(64)  NOT NULL,
  `chunk_index`   INT          NOT NULL COMMENT '分片序号（从 0 开始）',
  `chunk_size`    BIGINT       NOT NULL COMMENT '分片大小',
  `chunk_hash`    VARCHAR(64)  DEFAULT NULL COMMENT '分片 hash',
  `status`        INT          NOT NULL DEFAULT 0 COMMENT '0-待上传 1-已上传 2-失败',
  `storage_path`  VARCHAR(500) DEFAULT NULL COMMENT '临时存储路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_chunk` (`task_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分片上传详情';

CREATE TABLE IF NOT EXISTS `share_file` (
  `share_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '分享者 ID',
  `user_file_id` BIGINT       NOT NULL COMMENT '关联 user_file',
  `share_code`   VARCHAR(8)   NOT NULL COMMENT '8 位随机分享码',
  `extract_code` VARCHAR(6)   DEFAULT NULL COMMENT '提取码（NULL 表示公开）',
  `expire_time`  DATETIME     DEFAULT NULL COMMENT '过期时间（NULL 表示永久）',
  `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享';

CREATE TABLE IF NOT EXISTS `storage_bean` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL COMMENT '用户 ID',
  `total_quota`   BIGINT       NOT NULL DEFAULT 10737418240 COMMENT '总配额（字节），默认 10GB',
  `used_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '已用空间（字节）',
  `pre_used_size` BIGINT       NOT NULL DEFAULT 0 COMMENT '预扣空间（字节）',
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户存储配额';

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL,
  `user_file_id` BIGINT       NOT NULL,
  `action`       VARCHAR(20)  NOT NULL COMMENT '操作类型 download/share_download',
  `ip_address`   VARCHAR(45)  NOT NULL,
  `user_agent`   VARCHAR(500) DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_file_time` (`user_file_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

-- ============================================================
-- 模块三：admin（后台管理）
-- ============================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         VARCHAR(32)  NOT NULL COMMENT '操作者业务 ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '操作者用户名',
  `module`          VARCHAR(50)  NOT NULL COMMENT '模块名',
  `action`          VARCHAR(20)  NOT NULL COMMENT '操作类型 CREATE/UPDATE/DELETE',
  `description`     VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `request_method`  VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法',
  `request_uri`     VARCHAR(255) NOT NULL COMMENT '请求 URI',
  `request_params`  TEXT         DEFAULT NULL COMMENT '请求参数 JSON',
  `response_code`   INT          NOT NULL COMMENT '响应状态码',
  `error_message`   TEXT         DEFAULT NULL COMMENT '异常信息',
  `ip_address`      VARCHAR(50)  DEFAULT NULL COMMENT '客户端 IP',
  `user_agent`      VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `execution_time`  BIGINT       NOT NULL COMMENT '执行耗时 ms',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS `system_config` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `config_key`    VARCHAR(100) NOT NULL COMMENT '参数键名',
  `config_value`  VARCHAR(500) NOT NULL COMMENT '参数值',
  `description`   VARCHAR(200) DEFAULT NULL COMMENT '参数描述',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

-- 初始系统参数
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
  ('default.storage.quota', '10737418240', '默认存储配额（字节），10GB'),
  ('upload.max.size', '104857600', '单文件上传最大大小（字节），100MB'),
  ('upload.chunk.size', '5242880', '分片上传分片大小（字节），5MB'),
  ('share.default.expire.days', '7', '分享链接默认有效期（天）');

-- ============================================================
-- 模块四：document（文档管理）
-- ============================================================

CREATE TABLE IF NOT EXISTS `document_version` (
  `version_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `user_file_id`   BIGINT       NOT NULL,
  `file_id`        BIGINT       NOT NULL COMMENT '指向 file_bean.file_id',
  `version_number` INT          NOT NULL,
  `file_size`      BIGINT       NOT NULL,
  `editor_id`      BIGINT       NOT NULL COMMENT '编辑者 user_id',
  `type`           VARCHAR(16)  NOT NULL DEFAULT 'EDIT' COMMENT '操作类型 EDIT/RESTORE',
  `create_time`    DATETIME     NOT NULL,
  PRIMARY KEY (`version_id`),
  INDEX `idx_user_file_version` (`user_file_id`, `version_number`),
  INDEX `idx_editor` (`editor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档编辑版本历史';
