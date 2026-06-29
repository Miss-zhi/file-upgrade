# Docker 容器化部署

## 文件清单

| 文件 | 说明 |
|---|---|
| `docker-compose.yml` | 5 服务编排 |
| `qiwen-file/Dockerfile` | 后端多阶段构建 |
| `qiwen-file-web/Dockerfile` | 前端 nginx 构建 |
| `qiwen-file-web/nginx.conf` | SPA 路由 + API 代理 |
