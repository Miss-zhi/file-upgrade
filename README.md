# 奇文网盘 (Qiwen File)

> Spring Boot 3.2 + Vue 3.4 全栈文件管理平台

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring_Boot-3.2-green)](https://spring.io/)
[![Vue 3.4](https://img.shields.io/badge/Vue-3.4-blue)](https://vuejs.org/)
[![Tests](https://img.shields.io/badge/tests-57_passing-brightgreen)](qiwen-file/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## ✨ 功能

| 模块 | 功能 |
|---|---|
| 📁 文件管理 | 上传/下载/删除/移动/复制/重命名 |
| 📂 分类浏览 | 按类型筛选（图片/文档/视频/音乐/其他） |
| 🔍 搜索 | 全文检索 |
| 🔗 分享 | 链接分享 + 提取码 + 有效期 |
| ♻️ 回收站 | 软删除 + 恢复 + 彻底删除 |
| 📜 版本历史 | 文件版本快照 + 回滚 |
| 📤 分片上传 | 大文件分片 + 进度追踪 |
| ✅ 批量操作 | 多选删除/移动 |
| 📝 OnlyOffice | 在线文档编辑 |
| 👤 用户管理 | 注册/登录/JWT + RBAC 权限 |
| 📢 通知公告 | 系统公告发布 + 查看 |
| 📊 管理面板 | 统计仪表盘 + 操作日志 |
| ☁️ 多存储 | 本地/AliyunOSS/MinIO/FastDFS/七牛 |

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2 / Java 17 / MyBatis-Plus + JPA / Spring Security + JWT |
| 前端 | Vue 3.4 / Vite 5 / Element Plus / Pinia / TypeScript |
| 数据库 | MySQL 8.0 / Redis 7 |
| 搜索引擎 | Elasticsearch |
| 存储 | UFOP 抽象层（本地/阿里云/MinIO/FastDFS/七牛） |
| 测试 | JUnit 5 / H2 |
| 部署 | Docker / Docker Compose |

## 🚀 快速开始

### 前置条件

- JDK 17+
- Node.js 20+
- MySQL 8.0
- Redis 7
- Maven 3.9+

### 开发模式

```bash
# 1. 克隆项目
git clone <repo-url> && cd file-upgrade

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE qiwen_file DEFAULT CHARACTER SET utf8mb4;"

# 3. 启动后端
cd qiwen-file
mvn spring-boot:run -Dspring.profiles.active=dev

# 4. 启动前端（另开终端）
cd qiwen-file-web
npm install
npm run dev
# → http://localhost:5173
```

### Docker 一键部署

```bash
docker-compose up -d
# → http://localhost        (前端)
# → http://localhost:8080   (API)
# → http://localhost:8090   (OnlyOffice)
```

## 📁 项目结构

```
file-upgrade/
├── qiwen-file/                    # 后端 (Spring Boot)
│   ├── src/main/java/com/qiwenshare/file/
│   │   ├── api/                   # 服务接口
│   │   ├── config/                # Security/JWT/MyBatisPlus
│   │   ├── controller/            # REST 控制器
│   │   ├── domain/                # 实体 (JPA + MyBatis-Plus)
│   │   ├── mapper/                # MyBatis Mapper
│   │   ├── service/               # 业务实现
│   │   ├── vo/                    # 视图对象
│   │   └── util/                  # 工具类
│   ├── src/test/                  # 单元测试 (57 用例)
│   └── src/main/java/com/qiwenshare/ufop/
│       ├── constant/              # 枚举 (StorageType等)
│       ├── config/                # 存储配置
│       ├── domain/                # 存储实体
│       ├── exception/             # 操作异常
│       ├── operation/             # 8种操作 × 5存储实现
│       └── util/                  # 存储工具类
│
├── qiwen-file-web/                # 前端 (Vue 3)
│   ├── src/
│   │   ├── api/                   # API 请求
│   │   ├── components/            # 组件
│   │   ├── router/                # 路由
│   │   ├── stores/                # Pinia 状态管理
│   │   └── views/                 # 页面
│   └── nginx.conf                 # Nginx 配置
│
├── docker-compose.yml             # Docker 编排
├── openspec/                      # OpenSpec 变更记录 (26 个)
└── README.md
```

## 📡 API 简表

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/register` | 用户注册 |
| POST | `/user/login` | JWT 登录 |
| GET | `/file/list?path=/` | 文件列表 |
| POST | `/filetransfer/upload-chunk` | 分片上传 |
| POST | `/filetransfer/merge-chunks` | 合并分片 |
| DELETE | `/file/delete/{id}` | 软删除 |
| POST | `/file/batch-delete` | 批量删除 |
| GET | `/{fileId}/versions` | 版本历史 |
| GET | `/filetypes` | 文件分类 |
| POST | `/share/create` | 创建分享 |
| GET | `/admin/logs` | 操作日志 |
| GET | `/admin/roles` | 角色管理 |

> 完整 API 文档：启动后端后访问 http://localhost:8080/swagger-ui.html

## 🧪 测试

```bash
cd qiwen-file
mvn test -Dspring.profiles.active=test
# Tests run: 57, Failures: 0
```

## 📄 License

MIT
