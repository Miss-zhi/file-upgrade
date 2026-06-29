# Design: Docker 容器化部署

## 1. docker-compose.yml

**文件**: `E:\file-upgrade\docker-compose.yml`

```yaml
version: "3.8"
services:
  mysql:     # MySQL 8.0 — 数据持久化
  redis:     # Redis 7-alpine — 缓存
  backend:   # Spring Boot 3.2 — 多阶段构建
  frontend:  # Nginx + Vue 3 — SPA 静态文件
  onlyoffice:# OnlyOffice Document Server
volumes: mysql_data, redis_data, upload_data, oo_data, oo_lib
```

## 2. 后端 Dockerfile

**文件**: `qiwen-file/Dockerfile`

```dockerfile
# Stage 1: maven:3.9-eclipse-temurin-17 — 预下载依赖 + mvn package
# Stage 2: eclipse-temurin:17-jre-alpine — 非 root 用户运行
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
```

## 3. 前端 Dockerfile

**文件**: `qiwen-file-web/Dockerfile`

```dockerfile
# Stage 1: node:20-alpine — npm ci + npm run build
# Stage 2: nginx:1.25-alpine — 复制 dist + nginx.conf
CMD ["nginx", "-g", "daemon off;"]
EXPOSE 80
```

## 4. Nginx 配置

**文件**: `qiwen-file-web/nginx.conf`

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # API 反向代理 → backend:8080
    location /api/ { proxy_pass http://backend:8080/; }

    # OnlyOffice 反向代理
    location /onlyoffice/ { proxy_pass http://onlyoffice:80/; }

    # SPA 回退
    location / { try_files $uri $uri/ /index.html; }

    # 静态资源强缓存
    location /assets/ { expires 30d; }
}
```

## 5. 服务拓扑

```
Browser → Nginx(80) → / → index.html (SPA)
                    → /api/ → Backend(8080) → MySQL(3306) + Redis(6379)
                    → /onlyoffice/ → OnlyOffice(80)
```

## 6. 环境变量注入

**Backend 容器**通过 `environment` 注入 Spring Boot 配置：
- `SPRING_DATASOURCE_URL/PASSWORD` — MySQL 连接
- `SPRING_REDIS_HOST/PORT` — Redis 连接
- `UFOP_STORAGE_TYPE=LOCAL` — 本地存储
- `UFOP_LOCAL_ROOT_PATH=/data/uploads` — 上传根目录

## 7. 健康检查

- MySQL: `mysqladmin ping`（10s 间隔）
- Redis: `redis-cli ping`（10s 间隔）
- Backend 依赖 MySQL + Redis 健康检查通过后启动

## 8. 部署命令

```bash
# 构建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend

# 停止
docker-compose down
```
