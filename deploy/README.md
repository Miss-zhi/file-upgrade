# 奇文网盘 Docker 部署指南

## 目录结构

```
deploy/
── docker-compose.yml          # 全栈编排（6 个服务）
├── Dockerfile.elasticsearch    # ES + IK 中文分词插件
├── .env.example                # 环境变量模板
├── deploy.sh                   # 一键部署脚本
└── README.md                   # 本文档

../qiwenshare-backend/
└── Dockerfile                  # 后端多阶段构建

../qiwenshare-frontend/
├── Dockerfile                  # 前端多阶段构建
└── docker/
    └── nginx.conf              # Nginx 配置
```

## 前置条件

- Docker 24+ 和 Docker Compose V2
- 服务器内存建议 >= 4GB（ES + MySQL + 后端）
- 开放端口: 80（前端）、443（HTTPS 可选）

## 快速部署

```bash
# 1. 进入部署目录
cd deploy

# 2. 复制并编辑环境变量
cp .env.example .env
vim .env    # 填写实际密码和域名

# 3. 一键部署
./deploy.sh
```

## 环境变量说明

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `MyS3cure!Root` |
| `DB_USERNAME` | 应用数据库用户 | `qiwenshare` |
| `DB_PASSWORD` | 应用数据库密码 | `MyS3cure!Db` |
| `REDIS_PASSWORD` | Redis 密码 | `MyS3cure!Redis` |
| `JWT_SECRET` | JWT 签名密钥（Base64） | 见 .env.example |
| `ONLYOFFICE_JWT_SECRET` | OnlyOffice JWT 密钥 | 至少 32 字符 |
| `CORS_ALLOWED_ORIGINS` | 前端域名 | `https://pan.example.com` |
| `COOKIE_SECURE` | Cookie Secure 标志 | HTTPS 用 `true` |
| `COOKIE_SAME_SITE` | Cookie SameSite | HTTPS 用 `None` |

## 服务端口

| 服务 | 容器内端口 | 宿主机映射 | 说明 |
|------|-----------|-----------|------|
| 前端 Nginx | 80 | 80 | 对外服务入口 |
| 后端 API | 8080 | 127.0.0.1:8080 | 仅本地，经 Nginx 代理 |
| MySQL | 3306 | 127.0.0.1:3306 | 仅本地 |
| Redis | 6379 | 127.0.0.1:6379 | 仅本地 |
| Elasticsearch | 9200 | 127.0.0.1:9200 | 仅本地 |
| OnlyOffice | 80 | 127.0.0.1:8021 | 仅本地 |

> 基础设施服务（MySQL/Redis/ES/OnlyOffice）仅绑定 127.0.0.1，不暴露到公网。

## HTTPS 配置

1. 将 SSL 证书放入 `deploy/nginx-ssl/` 目录
2. 修改 `frontend/docker/nginx.conf` 添加 443 监听和 SSL 配置
3. 设置 `COOKIE_SECURE=true` 和 `COOKIE_SAME_SITE=None`
4. 更新 `CORS_ALLOWED_ORIGINS` 为 https 地址

## 常用运维命令

```bash
# 查看服务状态
./deploy.sh status

# 查看日志
./deploy.sh logs           # 所有服务
./deploy.sh logs backend   # 仅后端

# 重启单个服务
docker compose restart backend

# 停止所有服务（保留数据）
./deploy.sh stop

# 完全清理（删除数据卷，谨慎！）
docker compose down -v

# 更新代码并重新部署
./deploy.sh update
```

## 数据备份

```bash
# MySQL 备份
docker exec qiwenshare-mysql mysqldump -u root -p qiwenshare > backup.sql

# 文件存储备份
docker run --rm -v qiwenshare_file-storage:/data -v $(pwd):/backup \
    alpine tar czf /backup/files-backup.tar.gz -C /data .

# 恢复 MySQL
docker exec -i qiwenshare-mysql mysql -u root -p qiwenshare < backup.sql
```

## 故障排查

```bash
# 查看某个服务的详细日志
docker compose logs -f --tail=500 backend

# 进入容器调试
docker exec -it qiwenshare-backend sh

# 检查服务健康状态
docker compose ps

# 测试后端连通性
curl http://localhost:8080/actuator/health
```
