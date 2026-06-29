# CD 持续部署流水线

## Why

当前项目 CI 流水线已完善（前端 lint→typecheck→build，后端 compile→test→package），且已具备 Dockerfile + docker-compose.yml。但缺少自动化 CD（持续部署）流程，每次发布需要手动构建镜像、推送、部署。需要一个标准的 GitHub Actions CD 流水线来自动化这一过程。

## What Changes

在 `.github/workflows/cd.yml` 新增 CD 流水线，分为两个阶段：

1. **Build & Push**：构建前后端 Docker 镜像并推送到私有镜像仓库
2. **Deploy**：SSH 到目标服务器，拉取最新镜像并重启容器

所有敏感信息（仓库地址、SSH 凭证）均通过 GitHub Secrets 注入，不硬编码。

## Capabilities

| 能力 | 说明 |
|------|------|
| 镜像构建 | 基于现有 Dockerfile 多阶段构建后端 JRE 镜像和前端 Nginx 镜像 |
| 镜像推送 | 推送到可配置的私有 Docker Registry |
| 自动部署 | SSH 到服务器执行 `docker compose pull && up -d` |
| 手动触发 | 支持 `workflow_dispatch` 手动触发，可选择跳过部署 |
| 安全 | 所有凭证通过 GitHub Secrets 注入，不暴露在代码中 |

## Impact

- 新增文件：`.github/workflows/cd.yml`
- 无需修改现有 CI 流程
- 需要用户在 GitHub 仓库配置以下 Secrets：
  - `REGISTRY_HOST`：镜像仓库地址
  - `REGISTRY_USERNAME`：仓库用户名
  - `REGISTRY_PASSWORD`：仓库密码
  - `DEPLOY_HOST`：部署服务器 IP
  - `DEPLOY_USER`：SSH 用户名
  - `DEPLOY_SSH_KEY`：SSH 私钥
