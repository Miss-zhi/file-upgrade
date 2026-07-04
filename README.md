# 奇文网盘（QiWen Share）

企业级文件管理系统，支持多存储后端、在线文档协作、全文检索等功能。

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | records / sealed classes / text blocks |
| Spring Boot | 3.2.5 | Spring Framework 6.1 |
| Spring Security | 6.x | SecurityFilterChain，JWT 双 Token 机制 |
| JPA + MyBatis-Plus | 6.x / 3.5.7 | JPA 负责领域模型，MyBatis-Plus 负责复杂查询 |
| Elasticsearch | 8.13.0 | 全文检索，IK 中文分词 |
| JWT | jjwt 0.12.6 | Access Token + Refresh Token |
| MySQL | 8.0 | 主数据库 |
| Redis | 7 | 缓存 / 分布式锁 / 会话管理 |
| OnlyOffice | 8.2 | 在线文档协作编辑 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | Composition API + `<script setup lang="ts">` |
| Element Plus | 2.14 | UI 组件库 |
| Vite | 8.x | 构建工具 |
| Pinia | 3.x | 状态管理 |
| TypeScript | 6.x | 全量类型检查 |
| CodeMirror | 6.x | 代码文件预览 |

---

## 项目结构

```
file-upgrade/
├── deploy/                        # 部署配置
│   ├── docker-compose.yml         # 全栈服务编排
│   ├── .env.example               # 环境变量模板
│   └── Dockerfile.elasticsearch   # ES + IK 分词插件构建
│
├── qiwenshare-backend/            # 后端 Spring Boot 项目
│   ├── src/main/java/
│   │   └── com/qiwenshare/
│   │       ├── auth/              # 认证模块（JWT、登录、注册）
│   │       ├── file/              # 文件模块（上传、下载、管理）
│   │       ├── share/             # 分享模块
│   │       ├── search/            # 搜索模块（ES 全文检索）
│   │       ├── document/          # 文档模块（OnlyOffice 集成）
│   │       ├── storage/           # 存储模块（多后端适配）
│   │       ├── admin/             # 管理模块（用户、角色、系统配置）
│   │       ├── common/            # 公共模块（DTO、Entity、工具类）
│   │       └── config/            # 配置模块
│   ├── src/main/resources/
│   │   └── application-dev.yml    # 开发环境配置
│   ├── pom.xml
│   └── Dockerfile
│
└── qiwenshare-frontend/           # 前端 Vue 3 项目
    ├── src/
    │   ├── api/                   # API 请求层
    │   ├── components/            # 公共组件
    │   ├── composables/           # 组合式函数
    │   ├── layouts/               # 页面布局
    │   ├── router/                # 路由配置
    │   ├── stores/                # Pinia 状态管理
    │   ├── types/                 # TypeScript 类型定义
    │   ├── utils/                 # 工具函数
    │   └── views/                 # 页面视图
    ├── docker/nginx.conf          # Nginx 反向代理配置
    ├── package.json
    └── Dockerfile
```

---

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户认证 | 注册、登录、JWT 双 Token（Access + Refresh）、密码管理 |
| 文件管理 | 上传（含分片）、下载、移动、复制、重命名、删除、回收站恢复 |
| 文件分享 | 生成分享链接、设置有效期、密码保护、查看统计 |
| 在线预览 | 图片、视频、音频、代码、Markdown、Office 文档（OnlyOffice） |
| 全文检索 | Elasticsearch + IK 中文分词，按文件名/内容搜索 |
| 多存储后端 | 本地 / MinIO / 阿里云 OSS / 七牛云 / FastDFS，工厂模式统一抽象 |
| 管理后台 | 用户管理、角色权限（RBAC）、系统配置、审计日志、配额管理 |
| 文档协作 | OnlyOffice 集成，支持多人实时编辑 Word/Excel/PPT |

---

## 快速开始

### 环境要求

- Docker 24+ & Docker Compose v2+
- 服务器内存建议 ≥ 4GB（ES + OnlyOffice 较占内存）

### 部署步骤

**1. 克隆项目到服务器**

```bash
git clone <repo-url> /opt/file-upgrade
cd /opt/file-upgrade
```

**2. 配置环境变量**

```bash
cd deploy
cp .env.example .env
vim .env
```

必须修改以下配置项：

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `DB_PASSWORD` | 应用数据库密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | JWT 签名密钥（Base64 编码，解码后 ≥ 32 字节） |
| `ONLYOFFICE_JWT_SECRET` | OnlyOffice JWT 密钥（≥ 32 字符） |
| `CORS_ALLOWED_ORIGINS` | 前端访问域名，如 `https://pan.example.com` |
| `STORAGE_HOST_PATH` | 宿主机文件存储目录 |

生成 JWT 密钥：

```bash
echo -n "your-32-byte-secret-key-here" | base64
```

**3. 启动所有服务**

```bash
cd deploy
docker compose up -d
```

启动顺序由 healthcheck 自动编排：

```
MySQL / Redis / ES（并行）→ Backend → Frontend（Nginx）
OnlyOffice（独立启动）
```

**4. 验证服务状态**

```bash
docker compose ps
```

所有服务状态为 `healthy` 即部署成功。访问 `http://your-server-ip` 即可打开前端页面。

---

## 服务端口

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| Frontend（Nginx） | 80 / 443 | 80 / 443 | 对外入口，反向代理 API 和 OnlyOffice |
| Backend（Spring Boot） | 8080 | 127.0.0.1:8080 | 仅 Nginx 可访问 |
| MySQL | 3306 | 127.0.0.1:3306 | 仅宿主机访问 |
| Redis | 6379 | 127.0.0.1:6379 | 仅宿主机访问 |
| Elasticsearch | 9200 | 127.0.0.1:9200 | 仅宿主机访问 |
| OnlyOffice | 80 | 127.0.0.1:8021 | 仅 Nginx 可访问 |

---

## 存储后端配置

在 `.env` 中设置 `STORAGE_TYPE` 切换存储后端：

| 值 | 说明 |
|----|------|
| `local` | 本地磁盘，文件存储在 `STORAGE_HOST_PATH` 指定目录 |
| `minio` | MinIO 对象存储 |
| `aliyun` | 阿里云 OSS |
| `qiniu` | 七牛云对象存储 |
| `fastdfs` | FastDFS 分布式文件系统 |

---

## Nginx 反向代理路由

前端 Nginx 负责统一入口，路由规则：

| 路径 | 转发目标 | 说明 |
|------|---------|------|
| `/api/*` | `http://backend:8080` | 后端 REST API |
| `/onlyoffice/*` | `http://onlyoffice:80` | OnlyOffice 文档服务（含 WebSocket） |
| `/actuator/*` | `http://backend:8080` | 健康检查端点 |
| `/*` | `index.html` | SPA 路由回退 |

---

## 常用运维命令

```bash
# 查看所有服务状态
docker compose ps

# 查看某个服务日志
docker compose logs -f backend

# 重启单个服务
docker compose restart backend

# 重新构建并启动（代码更新后）
docker compose up -d --build backend frontend

# 停止所有服务
docker compose down

# 停止并清除数据卷（慎用，数据会丢失）
docker compose down -v
```

---

## HTTPS 配置（可选）

1. 将 SSL 证书放入 `deploy/nginx-ssl/` 目录：
   ```
   deploy/nginx-ssl/
   ├── your-domain.crt
   └── your-domain.key
   ```

2. 修改 `qiwenshare-frontend/docker/nginx.conf`，添加 443 监听和 SSL 配置

3. 在 `.env` 中设置：
   ```
   COOKIE_SECURE=true
   COOKIE_SAME_SITE=None
   CORS_ALLOWED_ORIGINS=https://your-domain.com
   ```

---

## 开发环境

### 后端

```bash
cd qiwenshare-backend

# 修改 src/main/resources/application-dev.yml 中的远程服务地址
# 然后启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端

```bash
cd qiwenshare-frontend

npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`，API 请求代理到后端地址。
