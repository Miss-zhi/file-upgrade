-- ============================================
-- 奇文网盘 初始化 SQL
-- MySQL 8.0 / utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS qiwen_file DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qiwen_file;

-- ============================================
-- 用户模块
-- ============================================

CREATE TABLE IF NOT EXISTS qiwen_user (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(200) NOT NULL COMMENT 'BCrypt密码',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    role        VARCHAR(20)  DEFAULT 'user' COMMENT '角色 admin/editor/user',
    status      INT          DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员 (BCrypt: admin123)
INSERT IGNORE INTO qiwen_user (id, username, password, nickname, role, status)
VALUES ('admin001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin', 1);

-- ============================================
-- RBAC 权限模块
-- ============================================

CREATE TABLE IF NOT EXISTS role (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(20)  NOT NULL UNIQUE COMMENT '角色名',
    description VARCHAR(100) DEFAULT NULL COMMENT '描述',
    available   INT          DEFAULT 1 COMMENT '1=可用 0=禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

INSERT IGNORE INTO role (role_id, role_name, description, available) VALUES
(1, 'admin',  '管理员', 1),
(2, 'editor', '编辑者', 1),
(3, 'viewer', '只读者', 1);

CREATE TABLE IF NOT EXISTS permission (
    permission_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id       BIGINT       DEFAULT NULL COMMENT '父权限ID',
    permission_name VARCHAR(30)  NOT NULL COMMENT '权限名称',
    resource_type   INT          DEFAULT 0 COMMENT '0=菜单 1=按钮',
    permission_code VARCHAR(30)  DEFAULT NULL COMMENT '权限码',
    order_num       INT          DEFAULT 0 COMMENT '排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

INSERT IGNORE INTO permission (permission_id, parent_id, permission_name, resource_type, permission_code, order_num) VALUES
(1,  NULL, '文件管理', 0, 'file', 1),
(2,  NULL, '用户管理', 0, 'user', 2),
(3,  NULL, '系统管理', 0, 'system', 3),
(4,  NULL, '管理面板', 0, 'dashboard', 4),
(5,  1,    '文件上传', 1, 'file:upload', 1),
(6,  1,    '文件下载', 1, 'file:download', 2),
(7,  1,    '文件删除', 1, 'file:delete', 3),
(8,  1,    '文件分享', 1, 'file:share', 4),
(9,  2,    '用户列表', 1, 'user:list', 1),
(10, 2,    '角色分配', 1, 'user:role', 2),
(11, 3,    '公告管理', 1, 'system:notice', 1);

CREATE TABLE IF NOT EXISTS user_role (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role_id BIGINT      NOT NULL COMMENT '角色ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 文件模块
-- ============================================

CREATE TABLE IF NOT EXISTS file_bean (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '文件ID',
    file_name   VARCHAR(200) NOT NULL COMMENT '文件名',
    file_path   VARCHAR(500) NOT NULL COMMENT '完整路径',
    file_size   BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    file_type   VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    is_folder   BOOLEAN      DEFAULT FALSE COMMENT '是否为文件夹',
    parent_path VARCHAR(500) DEFAULT '/' COMMENT '父路径',
    user_id     VARCHAR(64)  NOT NULL COMMENT '所属用户',
    deleted     INT          DEFAULT 0 COMMENT '0=正常 1=回收站',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_path (user_id, parent_path),
    INDEX idx_parent (parent_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ============================================
-- 文件分类
-- ============================================

CREATE TABLE IF NOT EXISTS file_type (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(50) NOT NULL COMMENT '类型名称',
    order_num INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件类型';

INSERT IGNORE INTO file_type (id, name, order_num) VALUES
(1, '图片', 1), (2, '文档', 2), (3, '视频', 3), (4, '音乐', 4), (5, '其他', 5);

CREATE TABLE IF NOT EXISTS file_classification (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    file_type_id     INT         NOT NULL COMMENT '文件类型ID',
    file_extend_name VARCHAR(25) NOT NULL COMMENT '扩展名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件扩展名分类';

INSERT IGNORE INTO file_classification (file_type_id, file_extend_name) VALUES
(1,'jpg'),(1,'jpeg'),(1,'png'),(1,'gif'),(1,'bmp'),(1,'webp'),(1,'svg'),
(2,'pdf'),(2,'doc'),(2,'docx'),(2,'xls'),(2,'xlsx'),(2,'ppt'),(2,'pptx'),(2,'txt'),(2,'md'),
(3,'mp4'),(3,'avi'),(3,'mkv'),(3,'mov'),(3,'wmv'),(3,'flv'),(3,'webm'),
(4,'mp3'),(4,'wav'),(4,'flac'),(4,'aac'),(4,'ogg'),(4,'wma');

-- ============================================
-- 文件版本历史
-- ============================================

CREATE TABLE IF NOT EXISTS file_version (
    id           VARCHAR(64)  NOT NULL PRIMARY KEY,
    file_id      VARCHAR(64)  NOT NULL COMMENT '文件ID',
    version      INT          NOT NULL COMMENT '版本序号',
    file_name    VARCHAR(200) NOT NULL,
    file_path    VARCHAR(500) NOT NULL,
    file_size    BIGINT       DEFAULT 0,
    storage_path VARCHAR(500) DEFAULT NULL COMMENT 'UFOP存储路径',
    user_id      VARCHAR(64)  NOT NULL,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件版本历史';

-- ============================================
-- 文件分享
-- ============================================

CREATE TABLE IF NOT EXISTS share_file (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    share_batch_num VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
    user_id         VARCHAR(64)  NOT NULL COMMENT '分享者',
    file_path       VARCHAR(500) NOT NULL COMMENT '文件路径',
    share_token     VARCHAR(64)  NOT NULL UNIQUE COMMENT '分享令牌',
    share_code      VARCHAR(10)  DEFAULT NULL COMMENT '提取码',
    expire_days     INT          DEFAULT 7,
    expire_time     DATETIME     DEFAULT NULL COMMENT '过期时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (share_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享文件表';

-- ============================================
-- 上传任务
-- ============================================

CREATE TABLE IF NOT EXISTS upload_task (
    id            VARCHAR(64)  NOT NULL PRIMARY KEY,
    identifier    VARCHAR(200) NOT NULL COMMENT 'MD5唯一标识',
    user_id       VARCHAR(64)  NOT NULL,
    file_name     VARCHAR(200) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    total_size    BIGINT       DEFAULT 0 COMMENT '总大小',
    chunk_num     INT          DEFAULT 0 COMMENT '已上传分片数',
    total_chunks  INT          DEFAULT 0 COMMENT '总分片数',
    upload_status INT          DEFAULT 0 COMMENT '0=进行中 1=完成 2=失败',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_identifier (identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传任务表';

-- ============================================
-- 操作日志
-- ============================================

CREATE TABLE IF NOT EXISTS operation_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(64)  DEFAULT NULL,
    username    VARCHAR(50)  DEFAULT NULL,
    operation   VARCHAR(50)  DEFAULT NULL COMMENT '操作名称',
    method      VARCHAR(200) DEFAULT NULL COMMENT '接口方法',
    params      TEXT         DEFAULT NULL COMMENT '请求参数',
    cost_time   BIGINT       DEFAULT 0 COMMENT '耗时ms',
    ip          VARCHAR(50)  DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 通知公告
-- ============================================

CREATE TABLE IF NOT EXISTS notice (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(100) NOT NULL COMMENT '标题',
    platform           INT          DEFAULT 3 COMMENT '平台 3=网盘',
    markdown_content   LONGTEXT     DEFAULT NULL COMMENT 'Markdown原文',
    content            LONGTEXT     DEFAULT NULL COMMENT 'HTML内容',
    valid_date_time    VARCHAR(25)  DEFAULT NULL COMMENT '有效期',
    is_long_valid_data INT          DEFAULT 0 COMMENT '0=否 1=长期有效',
    create_time        VARCHAR(25)  DEFAULT NULL,
    create_user_id     VARCHAR(20)  DEFAULT NULL,
    modify_time        VARCHAR(25)  DEFAULT NULL,
    modify_user_id     VARCHAR(20)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================
-- 系统配置
-- ============================================

CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT        DEFAULT NULL COMMENT '配置值'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT IGNORE INTO sys_config (config_key, config_value) VALUES
('site_name', '奇文网盘'),
('site_logo', ''),
('max_upload_size', '104857600'),
('default_expire_days', '7');
