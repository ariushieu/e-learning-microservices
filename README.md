# E-Learning Microservices

Hệ thống website học trực tuyến (E-Learning) được xây dựng theo kiến trúc **Microservices** với Spring Boot.
Đây là đồ án môn học *Kiến trúc Microservices* tại Trường Đại học Tài nguyên và Môi trường Hà Nội (HUNRE).

## Mục lục

- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Danh sách services](#danh-sách-services)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Hướng dẫn chạy](#hướng-dẫn-chạy)
- [Lộ trình phát triển](#lộ-trình-phát-triển)

## Kiến trúc tổng quan

```
                        ┌──────────────────┐
     Client (Web/App) ──►    API Gateway   │
                        └────────┬─────────┘
                                 │
      ┌──────────┬───────────────┼───────────────┬──────────────┐
      ▼          ▼               ▼               ▼              ▼
┌──────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────┐ ┌──────────────┐
│  Auth    │ │  Course  │ │ Enrollment  │ │  Quiz    │ │ Notification │
│ Service  │ │ Service  │ │  Service    │ │ Service  │ │   Service    │
└──────────┘ └──────────┘ └─────────────┘ └──────────┘ └──────────────┘
      │          │               │               │              │
      └──────────┴───────────────┴───────────────┴──────────────┘
                                 │
                        ┌────────▼─────────┐
                        │  shared-common   │  (DTO, exception, utils dùng chung)
                        └──────────────────┘
```

Mỗi service là một ứng dụng Spring Boot độc lập, có thể build, chạy và triển khai riêng.
Mọi request từ client đi qua **API Gateway** trước khi được điều hướng tới service tương ứng.

## Danh sách services

| Service                | Vai trò                                                                 | Port (dự kiến) |
|------------------------|-------------------------------------------------------------------------|----------------|
| `api-gateway`          | Cổng vào duy nhất, định tuyến request, xác thực token, rate limiting    | 8080           |
| `auth-service`         | Đăng ký, đăng nhập, quản lý người dùng, phân quyền (JWT)                | 8081           |
| `course-service`       | Quản lý khóa học, chương, bài học, tài liệu                             | 8082           |
| `enrollment-service`   | Đăng ký khóa học, theo dõi tiến độ học tập                              | 8083           |
| `quiz-service`         | Quản lý bài kiểm tra, câu hỏi, chấm điểm                                | 8084           |
| `notification-service` | Gửi thông báo (email / in-app) khi có sự kiện: ghi danh, hoàn thành...  | 8085           |
| `shared-common`        | Thư viện dùng chung: DTO, exception handler, tiện ích                   | -              |

## Công nghệ sử dụng

- **Ngôn ngữ:** Java 17
- **Framework:** Spring Boot 4.1.1
- **Build tool:** Maven (Maven Wrapper đi kèm trong từng service)
- **Dự kiến bổ sung:** Spring Cloud Gateway, Spring Security + JWT, Spring Data JPA, PostgreSQL/MySQL, Docker Compose, RabbitMQ/Kafka cho giao tiếp bất đồng bộ

## Cấu trúc thư mục

```
e-learning-microservices/
├── api-gateway/            # Spring Cloud Gateway
├── auth-service/           # Xác thực & người dùng
├── course-service/         # Khóa học
├── enrollment-service/     # Ghi danh & tiến độ
├── quiz-service/           # Bài kiểm tra
├── notification-service/   # Thông báo
├── shared-common/          # Mã nguồn dùng chung
├── .gitignore
└── README.md
```

Mỗi service có cấu trúc chuẩn Spring Boot:

```
<service>/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/hunre/<service>/
    │   └── resources/application.properties
    └── test/
```

## Yêu cầu môi trường

- JDK 17 trở lên
- Git
- (Tùy chọn) IntelliJ IDEA / VS Code
- (Sau này) Docker & Docker Compose

## Hướng dẫn chạy

Clone repository:

```bash
git clone https://github.com/ariushieu/e-learning-microservices.git
cd e-learning-microservices
```

Chạy một service bất kỳ bằng Maven Wrapper (ví dụ `auth-service`):

```bash
cd auth-service

# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Build ra file JAR:

```bash
./mvnw clean package
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

## Lộ trình phát triển

- [x] Khởi tạo skeleton cho 7 module Spring Boot
- [ ] Cấu hình port và `application.properties` cho từng service
- [ ] Cài đặt API Gateway với Spring Cloud Gateway
- [ ] Auth Service: đăng ký / đăng nhập, phát hành JWT
- [ ] Course Service: CRUD khóa học, bài học
- [ ] Enrollment Service: ghi danh, tiến độ
- [ ] Quiz Service: câu hỏi, bài kiểm tra, chấm điểm
- [ ] Notification Service: gửi thông báo qua message queue
- [ ] Docker Compose cho toàn bộ hệ thống
- [ ] Tài liệu API (Swagger / OpenAPI)

## Tác giả

- **Hieu Nguyen** – [@ariushieu](https://github.com/ariushieu)
