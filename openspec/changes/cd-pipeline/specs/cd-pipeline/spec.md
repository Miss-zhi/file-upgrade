# CD 持续部署流水线

## Purpose

为奇文网盘项目建立自动化 CD（持续部署）流水线，在代码推送到 main 分支时自动构建 Docker 镜像、推送到私有镜像仓库，并部署到目标服务器。

## ADDED Requirements

### Requirement 1: Docker 镜像自动构建

系统 SHALL 在代码推送后自动构建前后端 Docker 镜像，使用现有 Dockerfile 和 docker-compose.yml 配置。

#### Scenario: 代码推送到 main 分支自动触发构建

- **GIVEN** 开发者推送代码到 main 分支
- **WHEN** CI 流水线全部通过
- **THEN** CD 流水线自动构建后端 JRE 镜像和前端 Nginx 镜像
- **AND** 镜像标签同时包含 `latest` 和 commit SHA

#### Scenario: 手动触发构建

- **GIVEN** 开发者在 GitHub Actions 页面
- **WHEN** 点击 Run workflow 手动触发
- **THEN** CD 流水线执行构建，可选择是否跳过部署步骤

### Requirement 2: 镜像推送到私有仓库

系统 SHALL 将构建完成的镜像推送到指定的私有 Docker Registry。

#### Scenario: 成功推送镜像

- **GIVEN** 前后端镜像构建完成
- **WHEN** 执行 push 步骤
- **THEN** 镜像通过 `docker tag` 标记为仓库地址
- **AND** 使用 Secrets 凭据登录仓库
- **AND** 镜像成功推送到私有 Registry

### Requirement 3: SSH 远程部署

系统 SHALL 通过 SSH 连接到部署服务器，拉取最新镜像并重启容器。

#### Scenario: 自动部署到目标服务器

- **GIVEN** 镜像已成功推送到私有仓库
- **WHEN** 执行部署步骤
- **THEN** 通过 SSH 连接目标服务器
- **AND** 上传 docker-compose.yml
- **AND** 执行 `docker compose pull` 拉取最新镜像
- **AND** 执行 `docker compose up -d --remove-orphans` 重启服务

#### Scenario: 手动跳过部署

- **GIVEN** 通过 workflow_dispatch 手动触发
- **WHEN** 选择 skip_deploy = true
- **THEN** 构建和推送步骤正常执行
- **AND** 部署步骤被跳过

### Requirement 4: 安全凭证管理

系统 SHALL 通过 GitHub Secrets 管理所有敏感信息，不在代码中暴露任何凭证。

#### Scenario: 凭证通过 Secrets 注入

- **GIVEN** 仓库配置了 REGISTRY_HOST、REGISTRY_USERNAME 等 Secrets
- **WHEN** CD 流水线执行
- **THEN** 镜像仓库凭据通过 Secrets 读取
- **AND** SSH 私钥通过 DEPLOY_SSH_KEY Secret 注入
- **AND** 流水线日志中不输出明文凭证
