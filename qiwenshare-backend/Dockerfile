# ============================================================
# 奇文网盘后端 — 多阶段构建
# Stage 1: Maven 编译
# Stage 2: JDK 17 运行
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B && \
    ls -lh target/*.jar

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="qiwenshare"

# 时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/qiwenshare-backend-1.0.0-SNAPSHOT.jar app.jar

# 文件存储目录
RUN mkdir -p /data/qiwenshare/files && chown -R appuser:appgroup /data/qiwenshare/files
VOLUME /data/qiwenshare/files

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
