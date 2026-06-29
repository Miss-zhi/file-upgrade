# ---- Stage 1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
# 预下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B -q

COPY src/ ./src/
RUN mvn package -DskipTests -B -q

# ---- Stage 2: JRE 运行 ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
