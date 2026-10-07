# Biên bản kiểm thử course-service

- Chạy ngày **14:49:18 7/10/26 (UTC+7)**; commit nguồn: `b8ce705b0ff953484387568df2a034fd657398e2`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `1ce75ade20942c2bfc38c3e4fdef4abf3c61715bc630368a38157995269ec78e`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend gồm bản sửa lọc danh mục cha lấy cả khóa thuộc danh mục con; không đổi migration.
- Kết quả: **195/195 mã ca gốc đã chạy**, cộng 16 ca bổ sung;
  226 request chính (bao gồm chuẩn bị), 781 HTTP tính cả bước phụ,
  **703/703 assertion đạt**;
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
| COURSE-01.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/150`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/150`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/150`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/151`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791359358268-4c63b823-6cf4b4d8`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791359358268-4c63b823-6cf4b4d8`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791359358268-4c63b823-6cf4b4d8`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791359358268-4c63b823`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791359358268-4c63b823-9276d752`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/230`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/230`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/230`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/230`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/230`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791359358268-4c63b823-82801d06`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791359358268-4c63b823-02c7c12c`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791359358268-4c63b823-02c7c12c`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791359358268-4c63b823-02c7c12c`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791359358268-4c63b823`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791359358268-4c63b823-02c7c12c`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/230/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/230/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/230/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/230/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/289`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/288`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/288`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/288`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/289`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/290`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/290`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/290`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/157`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/158`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/159`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/160`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/161`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/162`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/163`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/164`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/165`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/166`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/167`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/168`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/169`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/170`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/172`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/173`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/236`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/237`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/238`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/239`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/240`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/241`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/242`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/243`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/244`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/29`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/246/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/247`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/248`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/249`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/250`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/251`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/252`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/253`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/254/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/255`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/256`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/257`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/258`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/259`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/260`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/261`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/262`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/263`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/292`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/304`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/310`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/320`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/327`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `b8ce705`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/290`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/232/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `b8ce705`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/232/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/232/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/232/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |
| COURSE-CATEGORY.1 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.1` |
| COURSE-CATEGORY.2 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.2` |
| COURSE-CATEGORY.3 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.3` |
| COURSE-CATEGORY.4 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.4` |
| COURSE-CATEGORY.5 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.5` |
| COURSE-CATEGORY.6 | `b8ce705`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.6` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
