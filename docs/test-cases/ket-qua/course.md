# Biên bản kiểm thử course-service

- Chạy ngày **15:36:28 7/10/26 (UTC+7)**; commit nguồn: `5c31231d7d85f828d05b098cf3b475bd397fb352`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `63068ff5a68c1f2ff0c880038b309f07faf3a40d155082c0116961635c652471`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend có đánh giá khóa học, tính lại số sao dưới khóa dòng và migration V5 lưu tên người viết.
- Kết quả: **220/220 mã ca gốc đã chạy**, cộng 16 ca bổ sung;
  251 request chính (bao gồm chuẩn bị), 854 HTTP tính cả bước phụ,
  **778/778 assertion đạt**;
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
- COURSE-25 kiểm vòng đời đánh giá, quyền theo sổ học viên, dữ liệu đầu vào, tên từ token,
  phân trang và ghi đồng thời qua gateway trên MySQL. Điểm lẻ không được tự làm tròn thành số sao.
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
| COURSE-01.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/182`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/182`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/182`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/183`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791362188916-22215ea3-d1ee8ba0`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791362188916-22215ea3-d1ee8ba0`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791362188916-22215ea3-d1ee8ba0`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791362188916-22215ea3`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791362188916-22215ea3-9edb6e2f`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/289`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/289`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/289`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/289`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/289`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791362188916-22215ea3-4bfce486`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791362188916-22215ea3-871430c2`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791362188916-22215ea3-871430c2`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791362188916-22215ea3-871430c2`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791362188916-22215ea3`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791362188916-22215ea3-871430c2`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/289/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/289/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/289/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/289/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/329`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/328`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/328`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/328`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/329`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/330`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/330`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/330`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/189`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/190`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/191`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/192`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/193`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/194`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/195`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/196`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/197`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/198`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/199`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/200`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/201`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/202`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/204`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/205`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/295`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/296`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/297`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/298`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/299`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/300`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/301`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/302`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/303`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/31`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/305/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/306`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/307`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/308`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/309`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/310`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/311`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/312`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/313/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/314`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/315`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/316`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/317`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/318`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/319`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/320`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/321`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/322`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/332`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/344`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/350`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/360`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/367`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `5c31231`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/330`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/291/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `5c31231`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/291/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/291/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/291/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |
| COURSE-CATEGORY.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.1` |
| COURSE-CATEGORY.2 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.2` |
| COURSE-CATEGORY.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.3` |
| COURSE-CATEGORY.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.4` |
| COURSE-CATEGORY.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.5` |
| COURSE-CATEGORY.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.6` |
| COURSE-25.1 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.1` |
| COURSE-25.2 | `5c31231`, native/gateway 8080 | 401 | **PASS** | PUT `/api/courses/332/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.2` |
| COURSE-25.3 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.3` |
| COURSE-25.4 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.4` |
| COURSE-25.5 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.5` |
| COURSE-25.6 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.6` |
| COURSE-25.7 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.7` |
| COURSE-25.8 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.8` |
| COURSE-25.9 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.9` |
| COURSE-25.10 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.10` |
| COURSE-25.11 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.11` |
| COURSE-25.12 | `5c31231`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/332/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.12` |
| COURSE-25.13 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.13` |
| COURSE-25.14 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.14` |
| COURSE-25.15 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.15` |
| COURSE-25.16 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.16` |
| COURSE-25.17 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.17` |
| COURSE-25.18 | `5c31231`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/9007199254740991/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.18` |
| COURSE-25.19 | `5c31231`, native/gateway 8080 | 401 | **PASS** | GET `/api/courses/332/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.19` |
| COURSE-25.20 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/332/status`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.20` |
| COURSE-25.21 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.21` |
| COURSE-25.22 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/auth/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.22` |
| COURSE-25.23 | `5c31231`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/332/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.23` |
| COURSE-25.24 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.24` |
| COURSE-25.25 | `5c31231`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/332`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.25` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
