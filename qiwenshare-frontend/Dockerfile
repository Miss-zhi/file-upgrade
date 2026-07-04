# ============================================================
# 奇文网盘前端 — 多阶段构建
# Stage 1: Node 编译
# Stage 2: Nginx 托管
# ============================================================

# ---- Stage 1: Build ----
FROM node:22-alpine AS builder
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

COPY . .

# 通过 build-arg 注入环境变量（VITE_ 前缀）
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_ONLYOFFICE_API_URL=/onlyoffice/web-apps/apps/api/documents/api.js

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_ONLYOFFICE_API_URL=$VITE_ONLYOFFICE_API_URL

RUN npm run build-only

# ---- Stage 2: Runtime ----
FROM nginx:1.27-alpine

# 时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 删除默认配置
RUN rm -rf /etc/nginx/conf.d/*

# 复制 Nginx 配置
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1
