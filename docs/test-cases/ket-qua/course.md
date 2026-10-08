# Biên bản kiểm thử course-service

- Chạy ngày **15:00:05 8/10/26 (UTC+7)**; commit nguồn: `696ee6c9444bea93bac9cdb294372bfb67df03c4`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 `2e76683bc4713fa29252cf4543ac3ec5d180b58e774b171640c1fff497163015`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend có đánh giá khóa học, tính lại số sao dưới khóa dòng và migration V5 lưu tên người viết.
- Kết quả: **279/279 mã ca gốc đã chạy**, cộng 16 ca bổ sung;
  310 request chính (bao gồm chuẩn bị), 1042 HTTP tính cả bước phụ,
  **973/973 assertion đạt**;
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
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
| COURSE-01.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.1` |
| COURSE-01.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.2` |
| COURSE-01.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.3` |
| COURSE-01.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.4` |
| COURSE-01.5 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.5` |
| COURSE-01.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.6` |
| COURSE-01.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-01.7` |
| COURSE-02.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.1` |
| COURSE-02.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.2` |
| COURSE-02.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.3` |
| COURSE-02.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.4` |
| COURSE-02.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.5` |
| COURSE-02.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/tree`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-02.6` |
| COURSE-03.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/280`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.1` |
| COURSE-03.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/280`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.2` |
| COURSE-03.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/280`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.3` |
| COURSE-03.4 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.4` |
| COURSE-03.5 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/categories/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.5` |
| COURSE-03.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/281`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-03.6` |
| COURSE-04.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791446405491-9acb07e8-48572bf6`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.1` |
| COURSE-04.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791446405491-9acb07e8-48572bf6`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.2` |
| COURSE-04.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791446405491-9acb07e8-48572bf6`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.3` |
| COURSE-04.4 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/absent-1791446405491-9acb07e8`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.4` |
| COURSE-04.5 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.5` |
| COURSE-04.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/slug/qa-1791446405491-9acb07e8-e9eb624d`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-04.6` |
| COURSE-08.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.1` |
| COURSE-08.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.2` |
| COURSE-08.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.3` |
| COURSE-08.4 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.4` |
| COURSE-08.5 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.5` |
| COURSE-08.6 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.6` |
| COURSE-08.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.7` |
| COURSE-08.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.8` |
| COURSE-08.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.9` |
| COURSE-08.10 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.10` |
| COURSE-08.11 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-08.11` |
| COURSE-09.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.1` |
| COURSE-09.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/446`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.2` |
| COURSE-09.3 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/446`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.3` |
| COURSE-09.4 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/446`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.4` |
| COURSE-09.5 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.5` |
| COURSE-09.6 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.6` |
| COURSE-09.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/446`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.7` |
| COURSE-09.8 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/446`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-09.8` |
| COURSE-10.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791446405491-9acb07e8-397be283`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.1` |
| COURSE-10.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791446405491-9acb07e8-fce288bc`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.2` |
| COURSE-10.3 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791446405491-9acb07e8-fce288bc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.3` |
| COURSE-10.4 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/khoa-qa-1791446405491-9acb07e8-fce288bc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.4` |
| COURSE-10.5 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/absent-1791446405491-9acb07e8`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.5` |
| COURSE-10.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/slug/khoa-qa-1791446405491-9acb07e8-fce288bc`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.6` |
| COURSE-10.7 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/slug/9223372036854775807`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-10.7` |
| COURSE-15.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.1` |
| COURSE-15.2 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/446/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.2` |
| COURSE-15.3 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/446/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.3` |
| COURSE-15.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/446/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.4` |
| COURSE-15.5 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/9007199254740991/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.5` |
| COURSE-15.6 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses/abc/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.6` |
| COURSE-15.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/446/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-15.7` |
| COURSE-19.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/457`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.1` |
| COURSE-19.2 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/456`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.2` |
| COURSE-19.3 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/456`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.3` |
| COURSE-19.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/456`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.4` |
| COURSE-19.5 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/9007199254740991`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.5` |
| COURSE-19.6 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/lessons/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.6` |
| COURSE-19.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/457`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.7` |
| COURSE-19.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/458`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.8` |
| COURSE-19.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/458`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.9` |
| COURSE-19.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/458`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-19.10` |
| COURSE-05.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.1` |
| COURSE-05.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.2` |
| COURSE-05.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.3` |
| COURSE-05.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.4` |
| COURSE-05.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.5` |
| COURSE-05.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.6` |
| COURSE-05.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.7` |
| COURSE-05.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-05.8` |
| COURSE-06.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/categories/287`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.1` |
| COURSE-06.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/288`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.2` |
| COURSE-06.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/289`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.3` |
| COURSE-06.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/290`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.4` |
| COURSE-06.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/291`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.5` |
| COURSE-06.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/292`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.6` |
| COURSE-06.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/293`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.7` |
| COURSE-06.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/294`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-06.8` |
| COURSE-07.1 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/295`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.1` |
| COURSE-07.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/296`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.2` |
| COURSE-07.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/297`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.3` |
| COURSE-07.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/298`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.4` |
| COURSE-07.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/299`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.5` |
| COURSE-07.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/300`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.6` |
| COURSE-07.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/categories/302`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.7` |
| COURSE-07.8 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/categories/303`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-07.8` |
| COURSE-11.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.1` |
| COURSE-11.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.2` |
| COURSE-11.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.3` |
| COURSE-11.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.4` |
| COURSE-11.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.5` |
| COURSE-11.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.6` |
| COURSE-11.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.7` |
| COURSE-11.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.8` |
| COURSE-11.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-11.9` |
| COURSE-12.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/452`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.1` |
| COURSE-12.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/453`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.2` |
| COURSE-12.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/454`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.3` |
| COURSE-12.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/455`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.4` |
| COURSE-12.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/456`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.5` |
| COURSE-12.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/457`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.6` |
| COURSE-12.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PUT `/api/courses/458`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.7` |
| COURSE-12.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/459`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.8` |
| COURSE-12.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/460`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.9` |
| COURSE-12.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/enrollments/80`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-12.10` |
| COURSE-13.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/462/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.1` |
| COURSE-13.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/463`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.2` |
| COURSE-13.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/464`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.3` |
| COURSE-13.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/465`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.4` |
| COURSE-13.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/466`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.5` |
| COURSE-13.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/467`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.6` |
| COURSE-13.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/468`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.7` |
| COURSE-13.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/469`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.8` |
| COURSE-13.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/470/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.9` |
| COURSE-13.10 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/471`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-13.10` |
| COURSE-14.1 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/472`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.1` |
| COURSE-14.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/473`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.2` |
| COURSE-14.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/474`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.3` |
| COURSE-14.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/475`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.4` |
| COURSE-14.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/476`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.5` |
| COURSE-14.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/477`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.6` |
| COURSE-14.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/478`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.7` |
| COURSE-14.8 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/479`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-14.8` |
| COURSE-16.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.1` |
| COURSE-16.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.2` |
| COURSE-16.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.3` |
| COURSE-16.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.4` |
| COURSE-16.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.5` |
| COURSE-16.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.6` |
| COURSE-16.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.7` |
| COURSE-16.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-16.8` |
| COURSE-17.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.1` |
| COURSE-17.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.2` |
| COURSE-17.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.3` |
| COURSE-17.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.4` |
| COURSE-17.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.5` |
| COURSE-17.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.6` |
| COURSE-17.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.7` |
| COURSE-17.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-17.8` |
| COURSE-18.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.1` |
| COURSE-18.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.2` |
| COURSE-18.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.3` |
| COURSE-18.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.4` |
| COURSE-18.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.5` |
| COURSE-18.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.6` |
| COURSE-18.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.7` |
| COURSE-18.8 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/460`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-18.8` |
| COURSE-20.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.1` |
| COURSE-20.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.2` |
| COURSE-20.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.3` |
| COURSE-20.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.4` |
| COURSE-20.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.5` |
| COURSE-20.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.6` |
| COURSE-20.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.7` |
| COURSE-20.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.8` |
| COURSE-20.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-20.9` |
| COURSE-21.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.1` |
| COURSE-21.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.2` |
| COURSE-21.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.3` |
| COURSE-21.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.4` |
| COURSE-21.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.5` |
| COURSE-21.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.6` |
| COURSE-21.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.7` |
| COURSE-21.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 5 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.8` |
| COURSE-21.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-21.9` |
| COURSE-22.1 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/472`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.1` |
| COURSE-22.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.2` |
| COURSE-22.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.3` |
| COURSE-22.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.4` |
| COURSE-22.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.5` |
| COURSE-22.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.6` |
| COURSE-22.7 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/lessons/478`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.7` |
| COURSE-22.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-22.8` |
| COURSE-23.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.1` |
| COURSE-23.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.2` |
| COURSE-23.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.3` |
| COURSE-23.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.4` |
| COURSE-23.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.5` |
| COURSE-23.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.6` |
| COURSE-23.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.7` |
| COURSE-23.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-23.8` |
| COURSE-24.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/488`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.1` |
| COURSE-24.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.2` |
| COURSE-24.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.3` |
| COURSE-24.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.4` |
| COURSE-24.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.5` |
| COURSE-24.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.6` |
| COURSE-24.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.7` |
| COURSE-24.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/495`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-24.8` |
| COURSE-SORT.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.1` |
| COURSE-SORT.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.2` |
| COURSE-SORT.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.3` |
| COURSE-SORT.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.4` |
| COURSE-SORT.5 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | GET `/api/courses`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-SORT.5` |
| COURSE-ARCHIVED.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/458`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.1` |
| COURSE-ARCHIVED.2 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/448/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.2` |
| COURSE-ARCHIVED.3 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/448/curriculum`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.3` |
| COURSE-ARCHIVED.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/448/curriculum`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.4` |
| COURSE-ARCHIVED.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/448/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-ARCHIVED.5` |
| COURSE-CATEGORY.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.1` |
| COURSE-CATEGORY.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.2` |
| COURSE-CATEGORY.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.3` |
| COURSE-CATEGORY.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.4` |
| COURSE-CATEGORY.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.5` |
| COURSE-CATEGORY.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-CATEGORY.6` |
| COURSE-25.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.1` |
| COURSE-25.2 | `696ee6c`, native/gateway 8080 | 401 | **PASS** | PUT `/api/courses/489/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.2` |
| COURSE-25.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.3` |
| COURSE-25.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.4` |
| COURSE-25.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.5` |
| COURSE-25.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.6` |
| COURSE-25.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.7` |
| COURSE-25.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.8` |
| COURSE-25.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.9` |
| COURSE-25.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.10` |
| COURSE-25.11 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.11` |
| COURSE-25.12 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/489/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.12` |
| COURSE-25.13 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.13` |
| COURSE-25.14 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.14` |
| COURSE-25.15 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.15` |
| COURSE-25.16 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.16` |
| COURSE-25.17 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.17` |
| COURSE-25.18 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/9007199254740991/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.18` |
| COURSE-25.19 | `696ee6c`, native/gateway 8080 | 401 | **PASS** | GET `/api/courses/489/reviews/me`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.19` |
| COURSE-25.20 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/489/status`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.20` |
| COURSE-25.21 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.21` |
| COURSE-25.22 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/auth/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.22` |
| COURSE-25.23 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | PATCH `/api/courses/489/status`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.23` |
| COURSE-25.24 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.24` |
| COURSE-25.25 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/489`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-25.25` |
| COURSE-26.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.1` |
| COURSE-26.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.2` |
| COURSE-26.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.3` |
| COURSE-26.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.4` |
| COURSE-26.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.5` |
| COURSE-26.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.6` |
| COURSE-26.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.7` |
| COURSE-26.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.8` |
| COURSE-26.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.9` |
| COURSE-26.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.10` |
| COURSE-26.11 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.11` |
| COURSE-26.12 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.12` |
| COURSE-26.13 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/lessons/496`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-26.13` |
| COURSE-27.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.1` |
| COURSE-27.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.2` |
| COURSE-27.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.3` |
| COURSE-27.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.4` |
| COURSE-27.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.5` |
| COURSE-27.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.6` |
| COURSE-27.7 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/9007199254740991/reviews/60`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.7` |
| COURSE-27.8 | `696ee6c`, native/gateway 8080 | 400 | **PASS** | DELETE `/api/courses/490/reviews/abc`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.8` |
| COURSE-27.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.9` |
| COURSE-27.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.10` |
| COURSE-27.11 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490/reviews/me`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.11` |
| COURSE-27.12 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.12` |
| COURSE-27.13 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.13` |
| COURSE-27.14 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.14` |
| COURSE-27.15 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.15` |
| COURSE-27.16 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.16` |
| COURSE-27.17 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/490`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-27.17` |
| COURSE-28.1 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.1` |
| COURSE-28.2 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.2` |
| COURSE-28.3 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.3` |
| COURSE-28.4 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.4` |
| COURSE-28.5 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.5` |
| COURSE-28.6 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.6` |
| COURSE-28.7 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.7` |
| COURSE-28.8 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.8` |
| COURSE-28.9 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.9` |
| COURSE-28.10 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.10` |
| COURSE-28.11 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.11` |
| COURSE-28.12 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.12` |
| COURSE-28.13 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.13` |
| COURSE-28.14 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | PUT `/api/courses/491/reviews/9007199254740991/reply`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.14` |
| COURSE-28.15 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.15` |
| COURSE-28.16 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.16` |
| COURSE-28.17 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.17` |
| COURSE-28.18 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.18` |
| COURSE-28.19 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.19` |
| COURSE-28.20 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.20` |
| COURSE-28.21 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.21` |
| COURSE-28.22 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491/reviews/me`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.22` |
| COURSE-28.23 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | DELETE `/api/courses/491/reviews/65/reply`; 2 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.23` |
| COURSE-28.24 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.24` |
| COURSE-28.25 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.25` |
| COURSE-28.26 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.26` |
| COURSE-28.27 | `696ee6c`, native/gateway 8080 | 200 | **PASS** | GET `/api/courses/491`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.27` |
| COURSE-28.28 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/491/reviews`; 3 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.28` |
| COURSE-28.29 | `696ee6c`, native/gateway 8080 | 404 | **PASS** | GET `/api/courses/491/reviews`; 4 kiểm tra; [JSON](course-evidence.json) → `COURSE-28.29` |

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

```bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
```

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
