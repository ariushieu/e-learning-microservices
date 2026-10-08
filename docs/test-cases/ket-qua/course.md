# Biên bản kiểm thử course-service

- Chạy ngày **08:07:35 8/10/26 (UTC+7)**; commit nguồn: `83e51b82f49612148db0909f11dd15b4d3386285`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `f6904214c1cbcb9f60cc70da33377bb6be6836f04d3e37be1c7047065e9fb00c`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend có đánh giá khóa học, tính lại số sao dưới khóa dòng và migration V5 lưu tên người viết.
- Kết quả: **233/233 mã ca gốc đã chạy**, cộng 16 ca bổ sung;
  264 request chính (bao gồm chuẩn bị), 880 HTTP tính cả bước phụ,
  **827/827 assertion đạt**;
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
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
| COURSE-01.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/218`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/218`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/218`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/219`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791421655221-9b548ab7-6c544788`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791421655221-9b548ab7-6c544788`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791421655221-9b548ab7-6c544788`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791421655221-9b548ab7`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791421655221-9b548ab7-a9d38774`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/353`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/353`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/353`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/353`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/353`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791421655221-9b548ab7-bf2b0067`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791421655221-9b548ab7-b2a9f245`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791421655221-9b548ab7-b2a9f245`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791421655221-9b548ab7-b2a9f245`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791421655221-9b548ab7`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791421655221-9b548ab7-b2a9f245`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/353/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/353/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/353/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/353/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/373`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/372`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/372`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/372`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/373`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/374`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/374`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/374`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/225`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/226`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/227`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/228`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/229`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/230`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/231`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/232`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/233`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/234`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/235`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/236`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/237`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/238`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/240`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/241`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/359`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/360`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/361`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/362`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/363`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/364`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/365`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/366`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/367`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/50`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/369/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/370`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/371`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/372`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/373`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/374`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/375`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/376`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/377/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/378`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/379`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/380`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/381`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/382`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/383`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/384`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/385`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/386`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/376`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/388`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/394`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/404`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/411`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `83e51b8`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/374`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/355/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/355/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/355/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/355/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |
| COURSE-CATEGORY.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.1` |
| COURSE-CATEGORY.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.2` |
| COURSE-CATEGORY.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.3` |
| COURSE-CATEGORY.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.4` |
| COURSE-CATEGORY.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.5` |
| COURSE-CATEGORY.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.6` |
| COURSE-25.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.1` |
| COURSE-25.2 | `83e51b8`, native/gateway 8080 | 401 | **PASS** | PUT `/api/courses/396/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.2` |
| COURSE-25.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.3` |
| COURSE-25.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.4` |
| COURSE-25.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.5` |
| COURSE-25.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.6` |
| COURSE-25.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.7` |
| COURSE-25.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.8` |
| COURSE-25.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.9` |
| COURSE-25.10 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.10` |
| COURSE-25.11 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.11` |
| COURSE-25.12 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/396/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.12` |
| COURSE-25.13 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.13` |
| COURSE-25.14 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.14` |
| COURSE-25.15 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.15` |
| COURSE-25.16 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.16` |
| COURSE-25.17 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.17` |
| COURSE-25.18 | `83e51b8`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/9007199254740991/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.18` |
| COURSE-25.19 | `83e51b8`, native/gateway 8080 | 401 | **PASS** | GET `/api/courses/396/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.19` |
| COURSE-25.20 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/396/status`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.20` |
| COURSE-25.21 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.21` |
| COURSE-25.22 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/auth/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.22` |
| COURSE-25.23 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/396/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.23` |
| COURSE-25.24 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.24` |
| COURSE-25.25 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/396`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.25` |
| COURSE-26.1 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.1` |
| COURSE-26.2 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.2` |
| COURSE-26.3 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.3` |
| COURSE-26.4 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.4` |
| COURSE-26.5 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.5` |
| COURSE-26.6 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.6` |
| COURSE-26.7 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.7` |
| COURSE-26.8 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.8` |
| COURSE-26.9 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.9` |
| COURSE-26.10 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.10` |
| COURSE-26.11 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.11` |
| COURSE-26.12 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.12` |
| COURSE-26.13 | `83e51b8`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/412`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.13` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
