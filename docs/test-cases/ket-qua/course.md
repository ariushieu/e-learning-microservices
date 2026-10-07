# Biên bản kiểm thử course-service

- Chạy ngày **12:48:18 7/10/26 (UTC+7)**; commit nguồn: `d3c98a1c57e32140c3aabaff7b27203dacd0ff8b`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `4412d06315bda462cf821530e65292335d9f2d62e95a4754c9aa138afb47912a`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend ở commit này giống mã backend `4f00a2c` đã vào main qua #51; thay đổi hiện tại là
  giao diện và tài liệu/collection, không sửa Java/migration.
- Kết quả: **195/195 mã ca gốc đã chạy**, cộng 10 ca bổ sung;
  220 request chính (bao gồm chuẩn bị), 762 HTTP tính cả bước phụ,
  **685/685 assertion đạt**;
  0 lỗi được Newman ghi nhận. HTTP và assertion từng ca nằm trong JSON bằng chứng.

## Hợp đồng và giới hạn

- Nhãn CHỜ trong course.md là kế hoạch cũ. Route/quyền sở hữu/nội dung đã vào #37;
  quyền học ARCHIVED vào #44; chống đếm trùng vào #51. Không sửa kỳ vọng của file tình huống.
- COURSE-19.8 dùng chính sách đã triển khai: 200 metadata, `content/contentUrl=null`, tài liệu rỗng.
  COURSE-24.7 trả 404 khi sai lesson cha và dữ liệu không đổi.
- COURSE-12.10 kiểm cả tên khóa mới trong response enrollment sau khi snapshot được cập nhật;
  HTTP 200 của PUT một mình không đủ để chứng minh Kafka đã đồng bộ.
- Ca bổ sung COURSE-ARCHIVED.5 cho phép 422 từ snapshot đã ARCHIVED hoặc 404 từ bước kiểm nguồn
  khi snapshot còn trễ. Phải xác nhận không có lượt ghi danh mới. Không chấp nhận 2xx/5xx.
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
| COURSE-01.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/123`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/123`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/123`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/124`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791352098590-25378c0e-4656aaf9`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791352098590-25378c0e-4656aaf9`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791352098590-25378c0e-4656aaf9`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791352098590-25378c0e`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791352098590-25378c0e-b80dc9c2`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/177`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/177`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/177`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/177`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/177`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791352098590-25378c0e-9276c782`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791352098590-25378c0e-b2651524`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791352098590-25378c0e-b2651524`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791352098590-25378c0e-b2651524`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791352098590-25378c0e`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791352098590-25378c0e-b2651524`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/177/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/177/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/177/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/177/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/249`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/248`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/248`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/248`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/249`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/250`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/250`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/250`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/130`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/131`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/132`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/133`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/134`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/135`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/136`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/137`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/138`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/139`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/140`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/141`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/142`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/143`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/145`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/146`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/183`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/184`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/185`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/186`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/187`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/188`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/189`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/190`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/191`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/27`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/193/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/194`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/195`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/196`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/197`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/198`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/199`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/200`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/201/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/202`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/203`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/204`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/205`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/206`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/207`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/208`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/209`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/210`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/252`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/264`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/270`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/280`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/287`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `d3c98a1`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/250`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/179/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `d3c98a1`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/179/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/179/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `d3c98a1`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/179/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
npx --package newman@6.2.2 newman run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
