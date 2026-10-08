# Biên bản QUIZ-17 — kết quả dành cho giảng viên

Ngày 08/10/2026. Code: `50acbeb36700d54cc88d2012eaa19613635bde50`, đã đồng bộ
`main` tới `a16f740` (#69). Các lần chạy dưới đây do người triển khai thực hiện trên máy này.

## Môi trường và phạm vi

- MySQL Community **8.0.46** cài trên Windows, instance kiểm thử riêng ở cổng 13306,
  database tạm riêng. Không dùng database hay service MySQL đang có của người dùng.
- Auth, course, enrollment, quiz và gateway chạy bằng các Application class đã build;
  xác thực JWT bật, đăng nhập qua auth API. Mọi request collection qua gateway **8080**.
- Flyway bật, Hibernate `ddl-auto=validate` cho cả 4 service có database. Không H2 trong
  lượt collection/UI này. SQL mode có `ONLY_FULL_GROUP_BY` và `STRICT_TRANS_TABLES`.
- Kafka listener/outbox worker và rate limit tắt bằng tham số tiến trình tạm. Nạp thủ công
  hai course snapshot vào database kiểm thử để thay phần đồng bộ Kafka. Bảy request thử
  dừng/bật Kafka được bỏ qua, không tính PASS. Docker/Kafka/Redis chạy chung: **chưa kiểm**.
- Frontend production build, chạy Next `start` cổng 3001. Bộ dữ liệu UI riêng gồm 21 học viên,
  1 lượt hết giờ, 1 lượt cũ chưa phân loại; ngoài ra kiểm trang từ bài thật do collection tạo.

## Kết quả

| Kiểm tra | Kết quả | Chi tiết |
|---|---|---|
| Toàn bộ Maven | PASS | `clean verify` trước khi đồng bộ #69: 786 test; `verify` sau đồng bộ: **807**, 0 failure/error/skip |
| Integration mới | PASS | **11 ca** với JWT, MockMvc, database H2 thật; không bọc test trong transaction |
| Collection toàn bộ | PASS | Newman 6.2.2, `runSlowTests=true`, timeout script 150000: **221 request, 492/492 assertions**, 0 lỗi script; 2 phút 8.5 giây |
| QUIZ-17.1–17.15 | PASS | 29 request gồm chuẩn bị; xem bằng A/admin, chặn B/S/khách, phân trang, làm lại, loại lượt thử, số hết giờ |
| QUIZ-13.7 | PASS | Chờ 100 giây → 422; MySQL lưu EXPIRED, score 0, submitted_at có giá trị |
| QUIZ-14.8 | PASS | Lượt chưa nộp trả 422, không đáp án |
| MySQL schema mới | PASS | Flyway V1–V4 success; Hibernate validate khớp entity; khởi động lại không lỗi checksum |
| MySQL nâng từ V3 | PASS | Database riêng dựng bằng SQL V1–V3, chèn bài SUBMITTED 50 điểm rồi áp dụng V4: điểm/trạng thái giữ nguyên, learner_name/is_preview NULL, index mới tồn tại |
| Frontend | PASS | `pnpm next typegen`, `pnpm tsc --noEmit`, `pnpm lint`, `pnpm build`; build/lint lại sau sửa responsive |
| Giao diện trình duyệt | PASS | **17/17** kiểm tra, xem `quiz-results/ui-checks.json` |
| Quy ước | PASS | Kiểm cấu hình bảo mật, migration đã merge, diff và commit hook |

Lượt collection chạy trước khi đồng bộ #69 (course resources); code backend quiz giữ nguyên.
Sau đồng bộ đã chạy lại toàn bộ Maven và build/kiểm giao diện production. Không ghi lượt này
thành Docker acceptance hoặc kiểm Kafka end-to-end.

## Đối chiếu dữ liệu MySQL

Đề `9016`: S nộp 50 rồi 0; B nộp 100; tác giả A và admin nộp 100 với `is_preview=1`.
API vẫn trả **2 học viên, 3 lượt nộp, trung bình 75, tỉ lệ đạt 50%**. Tỉ lệ đúng câu 1/2
theo tất cả lượt chấm là **66.67% / 33.33%**. Tên lấy từ token, không nhận tên từ query.
Đề `9015`, lượt `9032`: **EXPIRED, 0.00**, submitted_at `2026-10-08 02:12:51.384788` UTC.
API thống kê trả 1 lượt hết giờ, 0 lượt nộp; không lẫn vào điểm trung bình.

## UI và dữ liệu cũ

- Chủ đề/admin xem bảng; B chỉ thấy lỗi quyền, học viên thấy trang không có quyền; khách về đăng nhập.
- Trang đầu 20 học viên, trang sau 1; có link từ soạn đề. Tên dài và bảng cuộn trong thẻ,
  trang không tràn ngang ở **375 / 768 / 1366px**. Đã phát hiện và sửa tràn ngang ở 768px.
- Câu dưới 50% có nhãn "Cần xem xét"; chưa có bài nộp hiện EmptyState; có trạng thái đang tải.
- Dừng quiz-service: ErrorAlert + Thử lại, không giữ bảng cũ; bật lại và bấm Thử lại phục hồi.
- Bàn phím chuyển focus được; console không có lỗi JavaScript trong lượt kiểm tra trước khi thử mất kết nối.
- Quyết định cho dữ liệu cũ: vai trò lúc làm trước V4 không được lưu, nên **không đoán** lượt nào
  là admin làm thử. Lượt NULL phân loại bị loại khỏi thống kê, API trả số lượng bị loại và
  UI hiện cảnh báo. Điểm/lịch sử/kết quả cá nhân không bị xóa hoặc sửa. Resume không phân loại lại.

Bằng chứng: [API](quiz-results/api-checks.json), [UI](quiz-results/ui-checks.json),
[desktop](quiz-results/results-1366.png), [tablet](quiz-results/results-768.png),
[mobile](quiz-results/results-375.png), [403](quiz-results/forbidden.png).
