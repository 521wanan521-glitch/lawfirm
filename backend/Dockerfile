# 后端镜像：多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝 pom 缓存依赖，加速后续构建
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/lawfirm-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
