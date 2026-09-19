# =============================================================================
# Stage 1: Build JAR file
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper và toàn bộ pom.xml cấu trúc multi-module
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY shared-common/pom.xml shared-common/
COPY enrollment-service/pom.xml enrollment-service/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY course-service/pom.xml course-service/
COPY quiz-service/pom.xml quiz-service/
COPY notification-service/pom.xml notification-service/

# Cache dependencies
RUN ./mvnw dependency:go-offline -pl enrollment-service -am -B || true

# Copy mã nguồn của shared-common và enrollment-service
COPY shared-common shared-common/
COPY enrollment-service enrollment-service/

# Build gói JAR (bỏ qua kiểm thử trong lúc build Docker để tăng tốc)
RUN ./mvnw clean package -pl enrollment-service -am -DskipTests -B

# =============================================================================
# Stage 2: Runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user không có quyền root vì lý do an toàn bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/enrollment-service/target/enrollment-service-*.jar app.jar

USER appuser

EXPOSE 8083

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
