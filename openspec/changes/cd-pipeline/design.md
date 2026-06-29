# CD 流水线设计

## 流水线架构

```
push to main / 手动触发
        │
        ▼
┌─────────────────────┐
│  build-backend       │  多阶段构建后端 JRE 镜像
│  ─────────────────   │  标签: latest + commit-sha
│  Dockerfile          │
│  qiwen-file/         │
└────────┬────────────┘
         │ artifact: backend-image.tar
         ▼
┌─────────────────────┐
│  build-frontend      │  多阶段构建前端 Nginx 镜像
│  ─────────────────   │  标签: latest + commit-sha
│  Dockerfile          │
│  qiwen-file-web/     │
└────────┬────────────┘
         │ artifact: frontend-image.tar
         ▼
┌─────────────────────┐
│  push-images         │  加载镜像 → 重新打标签 → 推送
│  ─────────────────   │  needs: [build-backend, build-frontend]
│  推送到私有 Registry │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  deploy              │  SSH 到服务器 → docker compose pull → up -d
│  ─────────────────   │  needs: [push-images]
│  远程部署             │  if: 非手动跳过部署
└─────────────────────┘
```

## Job 详细设计

### 1. build-backend

- **runs-on**: ubuntu-latest
- **输出**: 构建 Docker 镜像，导出为 tar 文件作为 artifact
- **Docker build args**: 使用 buildx 缓存加速
- **标签策略**: `latest` + `${{ github.sha }}`（短 SHA）
- **文件路径**: `qiwen-file/Dockerfile`

### 2. build-frontend

- **runs-on**: ubuntu-latest
- **输出**: 构建 Docker 镜像，导出为 tar 文件作为 artifact
- **标签策略**: `latest` + `${{ github.sha }}`
- **文件路径**: `qiwen-file-web/Dockerfile`

### 3. push-images

- **needs**: [build-backend, build-frontend]
- **操作**: 
  1. 下载两个 image tar artifacts
  2. `docker load` 加载镜像
  3. `docker tag` 为私有仓库地址
  4. `docker login` 私有仓库
  5. `docker push` 推送镜像
- **凭证**: 从 Secrets 读取 `REGISTRY_*`

### 4. deploy

- **needs**: [push-images]
- **if**: `!inputs.skip_deploy`（手动触发时可跳过）
- **操作**:
  1. `ssh-agent` 加载 SSH 私钥
  2. `scp docker-compose.yml` 到服务器
  3. `ssh` 执行 `docker compose pull && docker compose up -d`
- **凭证**: 从 Secrets 读取 `DEPLOY_*`

## Secrets 配置

用户在 GitHub 仓库 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 | 示例 |
|--------|------|------|
| `REGISTRY_HOST` | 私有镜像仓库地址 | `registry.example.com` |
| `REGISTRY_USERNAME` | 仓库用户名 | `admin` |
| `REGISTRY_PASSWORD` | 仓库密码/token | `xxx` |
| `DEPLOY_HOST` | 部署服务器 IP/域名 | `192.168.1.100` |
| `DEPLOY_USER` | SSH 用户名 | `deploy` |
| `DEPLOY_SSH_KEY` | SSH 私钥（完整内容） | `-----BEGIN OPENSSH...` |

## 关键方法签名（GitHub Actions）

```
workflow: cd
  on: push → main, workflow_dispatch (inputs: skip_deploy)
  env:
    REGISTRY: ${{ secrets.REGISTRY_HOST }}
    BACKEND_IMAGE: ${{ secrets.REGISTRY_HOST }}/qiwen-backend
    FRONTEND_IMAGE: ${{ secrets.REGISTRY_HOST }}/qiwen-frontend
    IMAGE_TAG: ${{ github.sha }}  # 或 latest

job: build-backend
  steps:
    - checkout@v4
    - setup-qemu / setup-buildx
    - docker build -f qiwen-file/Dockerfile -t backend:$TAG .
    - docker save → upload-artifact

job: build-frontend
  steps:
    - checkout@v4
    - docker build -f qiwen-file-web/Dockerfile -t frontend:$TAG .
    - docker save → upload-artifact

job: push-images
  needs: [build-backend, build-frontend]
  steps:
    - download-artifact × 2 + docker load
    - docker tag + docker login + docker push

job: deploy
  needs: [push-images]
  steps:
    - ssh-agent + ssh-keyscan
    - scp docker-compose.yml
    - ssh docker compose pull && up -d --remove-orphans
```

## 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `.github/workflows/cd.yml` | 新增 | CD 流水线定义 |
