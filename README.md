# E-Learning Microservices

[![CI](https://github.com/ariushieu/e-learning-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/ariushieu/e-learning-microservices/actions/workflows/ci.yml)

Hệ thống website học trực tuyến (E-Learning) được xây dựng theo kiến trúc **Microservices** với Spring Boot.
Đây là sản phẩm môn học *Kiến trúc Microservices* tại Trường Đại học Tài nguyên và Môi trường Hà Nội (HUNRE).

## Mục lục

- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Danh sách services](#danh-sách-services)
- [Hạ tầng](#hạ-tầng)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Hướng dẫn chạy](#hướng-dẫn-chạy)
- [Lộ trình phát triển](#lộ-trình-phát-triển)

## Kiến trúc tổng quan

```
                        ┌──────────────────┐
     Client (Web/App) ──►    API Gateway   │  :8080
                        └────────┬─────────┘
                                 │
      ┌──────────┬───────────────┼───────────────┬──────────────┐
      ▼          ▼               ▼               ▼              ▼
┌──────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────┐ ┌──────────────┐
│  Auth    │ │  Course  │ │ Enrollment  │ │  Quiz    │ │ Notification │
│ Service  │ │ Service  │ │  Service    │ │ Service  │ │   Service    │
│  :8081   │ │  :8082   │ │   :8083     │ │  :8084   │ │    :8085     │
└────┬─────┘ └────┬─────┘ └──────┬──────┘ └────┬─────┘ └──────┬───────┘
     │            │              │              │              │
  auth_db     course_db    enrollment_db    quiz_db    notification_db
     └────────────┴──────────────┼──────────────┴──────────────┘
                                 │
                  ┌──────────────┴──────────────┐
                  │  MySQL :3306   Kafka :9092  │   (Docker Compose)
                  └─────────────────────────────┘
```

- Mỗi service là một ứng dụng Spring Boot độc lập, sở hữu **một database riêng** (database-per-service).
- Mọi request từ client đi qua **API Gateway** trước khi được điều hướng tới service tương ứng.
- Các service giao tiếp bất đồng bộ qua **Apache Kafka** (ví dụ: ghi danh thành công thì phát sự kiện để notification-service gửi thông báo).
- `shared-common` là thư viện dùng chung (DTO, exception, tiện ích), được các service import.

## Danh sách services

| Service                | Vai trò                                                                 | Port | Database          |
|------------------------|-------------------------------------------------------------------------|------|-------------------|
| `api-gateway`          | Cổng vào duy nhất, định tuyến request, xác thực token, rate limiting    | 8080 | -                 |
| `auth-service`         | Đăng ký, đăng nhập, quản lý người dùng, phân quyền (JWT)                | 8081 | `auth_db`         |
| `course-service`       | Quản lý khóa học, chương, bài học, tài liệu                             | 8082 | `course_db`       |
| `enrollment-service`   | Đăng ký khóa học, theo dõi tiến độ học tập                              | 8083 | `enrollment_db`   |
| `quiz-service`         | Quản lý bài kiểm tra, câu hỏi, chấm điểm                                | 8084 | `quiz_db`         |
| `notification-service` | Gửi thông báo (email / in-app) khi có sự kiện: ghi danh, hoàn thành...  | 8085 | `notification_db` |
| `shared-common`        | Thư viện dùng chung: DTO, exception handler, tiện ích (không chạy độc lập) | -  | -                 |

Mỗi service có endpoint kiểm tra sức khỏe tại `/actuator/health`.

## Hạ tầng

Hạ tầng dev chạy bằng Docker Compose (file `docker-compose.yml` ở gốc). Các service Spring Boot chạy ngoài Docker và kết nối qua `localhost`.

| Thành phần | Image                     | Cổng trên máy | Ghi chú                                                      |
|------------|---------------------------|---------------|--------------------------------------------------------------|
| MySQL      | `mysql:8.4`               | 3306          | Tự tạo 5 database khi khởi tạo lần đầu                       |
| Kafka      | `apache/kafka:4.3.1`      | 9092          | Chế độ KRaft, một node, không cần ZooKeeper                  |
| Kafka UI   | `ghcr.io/kafbat/kafka-ui` | 8090          | Xem topic, message, consumer group tại http://localhost:8090 |

Tài khoản MySQL mặc định: user `elearning` / mật khẩu `elearning`, root `root`. Đổi bằng cách sao chép `.env.example` thành `.env`.

Kafka có hai listener: ứng dụng trên máy host dùng `localhost:9092`; container khác trong mạng compose dùng `kafka:29092`.

## Công nghệ sử dụng

| Thành phần        | Công nghệ                                              |
|-------------------|--------------------------------------------------------|
| Ngôn ngữ          | Java 17                                                |
| Framework         | Spring Boot 4.1.1                                      |
| API Gateway       | Spring Cloud Gateway (Spring Cloud 2025.1.3 - Oakwood) |
| REST API          | Spring Web MVC, Bean Validation                        |
| Database          | MySQL 8.4 (database-per-service)                       |
| Message broker    | Apache Kafka 4.3 (KRaft)                               |
| Giám sát          | Spring Boot Actuator                                   |
| Tiện ích          | Lombok                                                 |
| Build             | Maven multi-module (parent POM ở thư mục gốc)         |
| Hạ tầng dev       | Docker Compose                                         |

**Dự kiến bổ sung:** Spring Security + JWT, Spring Data JPA, Spring for Apache Kafka, Swagger/OpenAPI, Dockerfile cho từng service, frontend Next.js.

## Cấu trúc thư mục

```
e-learning-microservices/
├── pom.xml                 # Parent POM: khai báo module, quản lý version chung
├── docker-compose.yml      # MySQL + Kafka + Kafka UI
├── .env.example            # Mẫu biến môi trường cho docker compose
├── infra/
│   └── mysql/init/         # SQL tạo database cho từng service
├── mvnw / mvnw.cmd         # Maven Wrapper
├── .mvn/
├── shared-common/          # Thư viện dùng chung (JAR thường, không có main)
├── api-gateway/            # Spring Cloud Gateway (WebFlux)
├── auth-service/           # Xác thực & người dùng
├── course-service/         # Khóa học
├── enrollment-service/     # Ghi danh & tiến độ
├── quiz-service/           # Bài kiểm tra
├── notification-service/   # Thông báo
├── .gitignore
└── README.md
```

Mỗi service có cấu trúc chuẩn Spring Boot:

```
<service>/
├── pom.xml                 # Kế thừa parent POM ở thư mục gốc
└── src/
    ├── main/
    │   ├── java/com/hunre/<service>/
    │   └── resources/application.properties
    └── test/
```

> **Lưu ý:** `api-gateway` chạy trên nền WebFlux (reactive). Không thêm `spring-boot-starter-webmvc`
> hoặc `shared-common` vào module này, vì sẽ kéo Tomcat vào và làm gateway khởi động sai chế độ.

## Yêu cầu môi trường

- JDK 17 trở lên (đã kiểm thử build với JDK 26)
- Docker Desktop (Docker Compose v2 trở lên)
- Git
- IntelliJ IDEA (khuyến nghị) hoặc VS Code

Không cần cài Maven, dự án dùng Maven Wrapper (`mvnw`).

## Hướng dẫn chạy

### 1. Clone repository

```bash
git clone https://github.com/ariushieu/e-learning-microservices.git
cd e-learning-microservices
```

### 2. Khởi động hạ tầng (MySQL, Kafka)

```bash
docker compose up -d
docker compose ps        # đợi cột STATUS hiện "healthy"
```

Kiểm tra nhanh:

```bash
# 5 database đã được tạo
docker exec elearning-mysql mysql -uelearning -pelearning -e "SHOW DATABASES;"

# Kafka nhận kết nối (Git Bash trên Windows: thêm MSYS_NO_PATHCONV=1 ở đầu lệnh)
docker exec elearning-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Mở http://localhost:8090 để xem Kafka UI.

Dừng hạ tầng: `docker compose down` (giữ dữ liệu) hoặc `docker compose down -v` (xóa hết dữ liệu).

### 3. Mở bằng IntelliJ IDEA

1. **File > Open**, chọn thư mục gốc `e-learning-microservices` (hoặc file `pom.xml` ở gốc).
2. IntelliJ tự nhận 7 module Maven. Đợi index và tải dependency xong.
3. **File > Project Structure > Project SDK**: chọn JDK 17 trở lên.
4. Chạy từng service bằng cách mở class `*Application.java` và nhấn Run. Không chạy `shared-common`.

### 4. Build và chạy bằng dòng lệnh

```bash
# Build toàn bộ (Windows dùng mvnw.cmd)
./mvnw clean package -DskipTests

# Chạy một service từ thư mục gốc
./mvnw -pl auth-service spring-boot:run

# Hoặc chạy bằng file JAR
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
```

### 5. Kiểm tra service đã lên

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

## Lộ trình phát triển

- [x] Khởi tạo skeleton cho 7 module Spring Boot
- [x] Cấu hình port cho từng service trong `application.properties`
- [x] Parent POM đa module, thêm dependency cơ bản (Web MVC, Validation, Actuator, Lombok, Spring Cloud Gateway)
- [x] Docker Compose cho hạ tầng dev: MySQL, Kafka (KRaft), Kafka UI
- [x] CI với GitHub Actions: build, test Maven và kiểm tra docker-compose cho mọi PR
- [ ] Kết nối database: Spring Data JPA + MySQL cho từng service
- [ ] Cấu hình route cho API Gateway tới các service
- [ ] Auth Service: đăng ký / đăng nhập, phát hành JWT
- [ ] Course Service: CRUD khóa học, bài học
- [ ] Enrollment Service: ghi danh, tiến độ, phát sự kiện Kafka
- [ ] Quiz Service: câu hỏi, bài kiểm tra, chấm điểm
- [ ] Notification Service: consume sự kiện Kafka, gửi thông báo
- [ ] Frontend Next.js (pnpm)
- [ ] Dockerfile cho từng service, chạy toàn bộ hệ thống bằng Docker Compose
- [ ] Tài liệu API (Swagger / OpenAPI)

## Tác giả

- **Hieu Nguyen** – [@ariushieu](https://github.com/ariushieu)
