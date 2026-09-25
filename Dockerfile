# syntax=docker/dockerfile:1
#
# Một Dockerfile dùng chung cho cả 6 service. Chọn service bằng build arg:
#
#   docker build --build-arg SERVICE=auth-service -t elearning/auth-service .
#
# Bình thường không cần gõ lệnh này: docker compose đã khai sẵn cho từng service, xem
# docker-compose.yml và phần "Chạy toàn bộ hệ thống bằng Docker" trong README.
#
# Vì sao một file chứ không phải mỗi service một file: sáu Dockerfile gần như giống hệt
# nhau, chỉ khác tên thư mục. Sáu bản sao thì sớm muộn cũng lệch — nâng phiên bản Java ở
# năm file mà quên file thứ sáu — và chẳng có gì báo lỗi cho tới lúc chạy.
#
# Build context PHẢI là thư mục gốc repo, không phải thư mục service: service nào cũng cần
# pom cha và shared-common để build được.

ARG SERVICE

# =============================================================================
# Giai đoạn 1: build jar từ mã nguồn
# =============================================================================
FROM eclipse-temurin:17-jdk AS build
ARG SERVICE
RUN test -n "$SERVICE" || (echo "Thiếu --build-arg SERVICE=<tên service>" >&2 && exit 1)

WORKDIR /src

# Maven đọc pom cha, và pom cha liệt kê cả 7 module, nên phải có đủ pom của cả 7 dù chỉ
# build một. Chép pom trước mã nguồn để Docker giữ được lớp cache khi chỉ sửa code.
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY shared-common/pom.xml        shared-common/
COPY api-gateway/pom.xml          api-gateway/
COPY auth-service/pom.xml         auth-service/
COPY course-service/pom.xml       course-service/
COPY enrollment-service/pom.xml   enrollment-service/
COPY quiz-service/pom.xml         quiz-service/
COPY notification-service/pom.xml notification-service/

COPY shared-common/src shared-common/src
COPY ${SERVICE}/src    ${SERVICE}/src

# --mount=type=cache giữ thư mục ~/.m2 giữa các lần build và giữa các service: thư viện
# tải một lần là cả sáu image dùng lại, không tải lại mỗi lần sửa một dòng code.
#
# Bỏ qua test ở đây vì CI đã chạy toàn bộ test cho mọi pull request. Image là để chạy, không
# phải chỗ kiểm tra lại lần hai.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
 && ./mvnw -B -q -pl "${SERVICE}" -am package -DskipTests \
 && cp "${SERVICE}"/target/"${SERVICE}"-*.jar /app.jar

# =============================================================================
# Giai đoạn 2: image chạy — chỉ JRE và đúng một file jar
# =============================================================================
FROM eclipse-temurin:17-jre
ARG SERVICE

# Không chạy bằng root. User "ubuntu" có sẵn trong image lại nằm trong nhóm sudo, nên tạo
# user hệ thống riêng, không nhóm phụ, không shell đăng nhập.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build --chown=app:app /app.jar app.jar
USER app

# Nhãn để "docker ps" và "docker images" nhìn là biết container nào chạy service nào.
LABEL org.opencontainers.image.title="${SERVICE}" \
      org.opencontainers.image.source="https://github.com/ariushieu/e-learning-microservices"

# MaxRAMPercentage: mặc định JVM chỉ lấy tối đa 25% bộ nhớ làm heap. Trong container có
# giới hạn bộ nhớ thì 25% quá ít; 75% để phần còn lại cho metaspace, thread và bộ đệm.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# sh -c để $JAVA_OPTS được thay giá trị; exec để java thay thế tiến trình shell, nhận được
# tín hiệu dừng của Docker và tắt êm thay vì bị giết sau 10 giây.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
