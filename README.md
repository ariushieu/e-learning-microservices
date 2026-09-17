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
     Client (Web/App) ──►    API Gateway   │  :8080
                        └────────┬─────────┘
                                 │
      ┌──────────┬───────────────┼───────────────┬──────────────┐
      ▼          ▼               ▼               ▼              ▼
┌──────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────┐ ┌──────────────┐
│  Auth    │ │  Course  │ │ Enrollment  │ │  Quiz    │ │ Notification │
│ Service  │ │ Service  │ │  Service    │ │ Service  │ │   Service    │
│  :8081   │ │  :8082   │ │   :8083     │ │  :8084   │ │    :8085     │
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

| Service                | Vai trò                                                                 | Port |
|------------------------|-------------------------------------------------------------------------|------|
| `api-gateway`          | Cổng vào duy nhất, định tuyến request, xác thực token, rate limiting    | 8080 |
| `auth-service`         | Đăng ký, đăng nhập, quản lý người dùng, phân quyền (JWT)                | 8081 |
| `course-service`       | Quản lý khóa học, chương, bài học, tài liệu                             | 8082 |
| `enrollment-service`   | Đăng ký khóa học, theo dõi tiến độ học tập                              | 8083 |
| `quiz-service`         | Quản lý bài kiểm tra, câu hỏi, chấm điểm                                | 8084 |
| `notification-service` | Gửi thông báo (email / in-app) khi có sự kiện: ghi danh, hoàn thành...  | 8085 |
| `shared-common`        | Thư viện dùng chung: DTO, exception handler, tiện ích (không chạy độc lập) | -  |

Mỗi service có endpoint kiểm tra sức khỏe tại `/actuator/health`.

## Công nghệ sử dụng

| Thành phần        | Công nghệ                                              |
|-------------------|--------------------------------------------------------|
| Ngôn ngữ          | Java 17                                                |
| Framework         | Spring Boot 4.1.1                                      |
| API Gateway       | Spring Cloud Gateway (Spring Cloud 2025.1.3 - Oakwood) |
| REST API          | Spring Web MVC, Bean Validation                        |
| Giám sát          | Spring Boot Actuator                                   |
| Tiện ích          | Lombok                                                 |
| Build             | Maven multi-module (parent POM ở thư mục gốc)         |

**Dự kiến bổ sung:** Spring Security + JWT, Spring Data JPA, PostgreSQL/MySQL, Docker Compose, RabbitMQ/Kafka cho giao tiếp bất đồng bộ, Swagger/OpenAPI.

## Cấu trúc thư mục

```
e-learning-microservices/
├── pom.xml                 # Parent POM: khai báo module, quản lý version chung
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
- Git
- IntelliJ IDEA (khuyến nghị) hoặc VS Code
- (Sau này) Docker & Docker Compose

Không cần cài Maven, dự án dùng Maven Wrapper (`mvnw`).

## Hướng dẫn chạy

### Clone repository

```bash
git clone https://github.com/ariushieu/e-learning-microservices.git
cd e-learning-microservices
```

### Mở bằng IntelliJ IDEA

1. **File > Open**, chọn thư mục gốc `e-learning-microservices` (hoặc file `pom.xml` ở gốc).
2. IntelliJ tự nhận 7 module Maven. Đợi index và tải dependency xong.
3. **File > Project Structure > Project SDK**: chọn JDK 17 trở lên.
4. Chạy từng service bằng cách mở class `*Application.java` và nhấn Run.

### Build toàn bộ dự án

```bash
# Windows
mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

### Chạy một service bằng Maven (từ thư mục gốc)

```bash
# Windows
mvnw.cmd -pl auth-service spring-boot:run

# Linux / macOS
./mvnw -pl auth-service spring-boot:run
```

### Chạy bằng file JAR

```bash
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
```

### Kiểm tra service đã lên

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

## Lộ trình phát triển

- [x] Khởi tạo skeleton cho 7 module Spring Boot
- [x] Cấu hình port cho từng service trong `application.properties`
- [x] Parent POM đa module, thêm dependency cơ bản (Web MVC, Validation, Actuator, Lombok, Spring Cloud Gateway)
- [ ] Cấu hình route cho API Gateway tới các service
- [ ] Auth Service: đăng ký / đăng nhập, phát hành JWT
- [ ] Course Service: CRUD khóa học, bài học
- [ ] Enrollment Service: ghi danh, tiến độ
- [ ] Quiz Service: câu hỏi, bài kiểm tra, chấm điểm
- [ ] Notification Service: gửi thông báo qua message queue
- [ ] Kết nối database (Spring Data JPA)
- [ ] Docker Compose cho toàn bộ hệ thống
- [ ] Tài liệu API (Swagger / OpenAPI)

## Tác giả

- **Hieu Nguyen** – [@ariushieu](https://github.com/ariushieu)
