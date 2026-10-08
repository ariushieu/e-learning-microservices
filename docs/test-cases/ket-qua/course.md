# Biên bản kiểm thử course-service

- Chạy ngày **21:51:36 8/10/26 (UTC+7)**; commit nguồn: `389fe4cd3c78c2c551852d8e3f4228745ece7ede`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `cab81dad74dbebd7474e82b45e043b2018716f30e1fda9ed3c0bae876fd21ec6`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend có đánh giá khóa học, tính lại số sao dưới khóa dòng và migration V5 lưu tên người viết.
- Kết quả: **317/317 mã ca gốc đã chạy**, cộng 16 ca bổ sung;
  348 request chính (bao gồm chuẩn bị), 1154 HTTP tính cả bước phụ,
  **1078/1078 assertion đạt**;
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
- COURSE-26 kiểm URL tài liệu HTTP/HTTPS, chặn giao thức nguy hiểm và URL sai cấu trúc,
  lỗi theo trường fileUrl, xác nhận không lưu dữ liệu sai và đọc lại sau khi xóa.
- COURSE-27 kiểm quyền admin gỡ đánh giá, sai khóa, điểm sau khi gỡ, người viết tạo lại,
  gỡ đồng thời với sửa điểm và gỡ ở trạng thái ARCHIVED/DRAFT trên MySQL.
- COURSE-28 kiểm quyền chủ khóa/admin phản hồi, độ dài và lỗi theo ô content, giữ phản hồi
  khi sửa sao, xóa theo đánh giá, không lộ phản hồi khóa nháp và sửa đồng thời trên MySQL.
- COURSE-29 kiểm inbox giảng viên/admin qua gateway, phân trang, lọc khóa/trạng thái,
  số chưa trả lời và nhãn phản hồi dựa trên người sở hữu khóa, không lộ ID nội bộ.
- COURSE-30 kiểm hồ sơ giảng viên công khai, điểm có trọng số, thống kê chỉ từ PUBLISHED,
  phân trang khóa, không lộ giảng viên chỉ có khóa nháp và cập nhật số liệu khi sửa/xóa đánh giá.
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
| COURSE-01.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/346`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/346`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/346`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/347`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791471096299-92ef47ab-6e5cf283`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791471096299-92ef47ab-6e5cf283`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791471096299-92ef47ab-6e5cf283`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791471096299-92ef47ab`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791471096299-92ef47ab-99b3454b`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/578`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/578`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/578`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/578`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/578`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791471096299-92ef47ab-aa9f002e`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791471096299-92ef47ab-c2335535`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791471096299-92ef47ab-c2335535`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791471096299-92ef47ab-c2335535`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791471096299-92ef47ab`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791471096299-92ef47ab-c2335535`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/578/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/578/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/578/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/578/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/539`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/538`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/538`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/538`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/539`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/540`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/540`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/540`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/353`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/354`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/355`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/356`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/357`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/358`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/359`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/360`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/361`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/362`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/363`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/364`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/365`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/366`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/368`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/369`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/584`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/585`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/586`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/587`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/588`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/589`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/590`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/591`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/592`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/120`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/594/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/595`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/596`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/597`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/598`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/599`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/600`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/601`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/602/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/603`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/604`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/605`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/606`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/607`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/608`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/609`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/610`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/611`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/542`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/554`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/560`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/570`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/577`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/540`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/580/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/580/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/580/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/580/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |
| COURSE-CATEGORY.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.1` |
| COURSE-CATEGORY.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.2` |
| COURSE-CATEGORY.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.3` |
| COURSE-CATEGORY.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.4` |
| COURSE-CATEGORY.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.5` |
| COURSE-CATEGORY.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.6` |
| COURSE-25.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.1` |
| COURSE-25.2 | `389fe4c`, native/gateway 8080 | 401 | **PASS** | PUT `/api/courses/621/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.2` |
| COURSE-25.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.3` |
| COURSE-25.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.4` |
| COURSE-25.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.5` |
| COURSE-25.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.6` |
| COURSE-25.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.7` |
| COURSE-25.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.8` |
| COURSE-25.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.9` |
| COURSE-25.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.10` |
| COURSE-25.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.11` |
| COURSE-25.12 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/621/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.12` |
| COURSE-25.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.13` |
| COURSE-25.14 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.14` |
| COURSE-25.15 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.15` |
| COURSE-25.16 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.16` |
| COURSE-25.17 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.17` |
| COURSE-25.18 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/9007199254740991/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.18` |
| COURSE-25.19 | `389fe4c`, native/gateway 8080 | 401 | **PASS** | GET `/api/courses/621/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.19` |
| COURSE-25.20 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/621/status`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.20` |
| COURSE-25.21 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.21` |
| COURSE-25.22 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/auth/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.22` |
| COURSE-25.23 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/621/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.23` |
| COURSE-25.24 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.24` |
| COURSE-25.25 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/621`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.25` |
| COURSE-26.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.1` |
| COURSE-26.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.2` |
| COURSE-26.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.3` |
| COURSE-26.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.4` |
| COURSE-26.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.5` |
| COURSE-26.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.6` |
| COURSE-26.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.7` |
| COURSE-26.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.8` |
| COURSE-26.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.9` |
| COURSE-26.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.10` |
| COURSE-26.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.11` |
| COURSE-26.12 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.12` |
| COURSE-26.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/578`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.13` |
| COURSE-27.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.1` |
| COURSE-27.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.2` |
| COURSE-27.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.3` |
| COURSE-27.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.4` |
| COURSE-27.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.5` |
| COURSE-27.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.6` |
| COURSE-27.7 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/9007199254740991/reviews/112`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.7` |
| COURSE-27.8 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | DELETE `/api/courses/622/reviews/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.8` |
| COURSE-27.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.9` |
| COURSE-27.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.10` |
| COURSE-27.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.11` |
| COURSE-27.12 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.12` |
| COURSE-27.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.13` |
| COURSE-27.14 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.14` |
| COURSE-27.15 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.15` |
| COURSE-27.16 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.16` |
| COURSE-27.17 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/622`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.17` |
| COURSE-28.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.1` |
| COURSE-28.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.2` |
| COURSE-28.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.3` |
| COURSE-28.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.4` |
| COURSE-28.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.5` |
| COURSE-28.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.6` |
| COURSE-28.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.7` |
| COURSE-28.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.8` |
| COURSE-28.9 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.9` |
| COURSE-28.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.10` |
| COURSE-28.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.11` |
| COURSE-28.12 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.12` |
| COURSE-28.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.13` |
| COURSE-28.14 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/623/reviews/9007199254740991/reply`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.14` |
| COURSE-28.15 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.15` |
| COURSE-28.16 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.16` |
| COURSE-28.17 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.17` |
| COURSE-28.18 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.18` |
| COURSE-28.19 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.19` |
| COURSE-28.20 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.20` |
| COURSE-28.21 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.21` |
| COURSE-28.22 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623/reviews/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.22` |
| COURSE-28.23 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/623/reviews/117/reply`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.23` |
| COURSE-28.24 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.24` |
| COURSE-28.25 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.25` |
| COURSE-28.26 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.26` |
| COURSE-28.27 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/623`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.27` |
| COURSE-28.28 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/623/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.28` |
| COURSE-28.29 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/623/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.29` |
| COURSE-29.1 | `389fe4c`, native/gateway 8080 | 401 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.1` |
| COURSE-29.2 | `389fe4c`, native/gateway 8080 | 403 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.2` |
| COURSE-29.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.3` |
| COURSE-29.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.4` |
| COURSE-29.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.5` |
| COURSE-29.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.6` |
| COURSE-29.7 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.7` |
| COURSE-29.8 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.8` |
| COURSE-29.9 | `389fe4c`, native/gateway 8080 | 403 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.9` |
| COURSE-29.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.10` |
| COURSE-29.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.11` |
| COURSE-29.12 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.12` |
| COURSE-29.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/624/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.13` |
| COURSE-29.14 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.14` |
| COURSE-29.15 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.15` |
| COURSE-29.16 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.16` |
| COURSE-29.17 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.17` |
| COURSE-29.18 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.18` |
| COURSE-29.19 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructor/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-29.19` |
| COURSE-30.1 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.1` |
| COURSE-30.2 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.2` |
| COURSE-30.3 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.3` |
| COURSE-30.4 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.4` |
| COURSE-30.5 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.5` |
| COURSE-30.6 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.6` |
| COURSE-30.7 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/instructors/94`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.7` |
| COURSE-30.8 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/instructors/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.8` |
| COURSE-30.9 | `389fe4c`, native/gateway 8080 | 400 | **PASS** | GET `/api/instructors/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.9` |
| COURSE-30.10 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.10` |
| COURSE-30.11 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/94`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.11` |
| COURSE-30.12 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.12` |
| COURSE-30.13 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.13` |
| COURSE-30.14 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.14` |
| COURSE-30.15 | `389fe4c`, native/gateway 8080 | 200 | **PASS** | GET `/api/instructors/93`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.15` |
| COURSE-30.16 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/instructors/93`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.16` |
| COURSE-30.17 | `389fe4c`, native/gateway 8080 | 404 | **PASS** | GET `/api/instructors/93`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.17` |
| COURSE-30.18 | `389fe4c`, native/gateway 8080 | 401 | **PASS** | POST `/api/instructors/94`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.18` |
| COURSE-30.19 | `389fe4c`, native/gateway 8080 | 401 | **PASS** | GET `/api/instructor/reviews`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-30.19` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
