# Nghiệm thu ENROLL-13 — quiz tự hoàn thành bài học

Ngày kiểm: 08/10/2026. PR: [#94](https://github.com/ariushieu/e-learning-microservices/pull/94).
Code nghiệp vụ được kiểm ở `938a79bdc86356fd4ae71727bdf067cb050beadd`, sau đó xác nhận lại
trên `8302c773bf4f09462bc6601bd1f17737f7661a8d` (chỉ sửa runner và cách chụp ảnh).

## Kết quả đã xác nhận

| Phạm vi | Kết quả | Bằng chứng |
|---|---|---|
| Maven toàn reactor trên máy | **PASS** — 1.048 test, 0 failures/errors/skips; JDK 21.0.12, target Java 17 | `clean verify`, kết thúc 21:59:37 UTC+7 |
| Maven CI JDK 17 | **PASS** | [CI 37799408115](https://github.com/ariushieu/e-learning-microservices/actions/runs/37799408115) |
| MySQL schema/Flyway V4 | **PASS** | Job `Schema matches entities (MySQL)` của CI trên |
| Frontend | **PASS** — typegen, TypeScript, ESLint, build production trên máy và CI | Job `Frontend (Next.js)` |
| Collection enrollment trên Docker | **PASS** — 519/519 assertion, 280 HTTP request kể cả polling và hai PUT đồng thời; 252 request định nghĩa trong collection | [docker-http.json](enrollment-quiz-progress/docker-http.json), 22:20:32–22:21:43 UTC+7 |
| Chromium trên Docker | **PASS** — hướng dẫn quiz, đạt rồi Back về trang học tự cập nhật, liên kết chứng chỉ, 3 kích thước | [quiz-progress-browser.json](enrollment-quiz-progress/quiz-progress-browser.json), 22:22:21 UTC+7 |
| ENROLL-09 hồi quy | **PASS** — 5/5 ca MySQL/Kafka cũ | [Resilience 37797405871](https://github.com/ariushieu/e-learning-microservices/actions/runs/37797405871) |

[Artifact API và giao diện](https://github.com/ariushieu/e-learning-microservices/actions/runs/37799408115/artifacts/11560252179)
chứa báo cáo đã loại token và sáu ảnh trước/sau ở 1366, 768, 375 px. Collection SHA-256:
`9b9bc03a07393c3aac2ec40c6947992a1910b472aec495ce0fa6f240cad5d392`.

| Màn hình | Trước khi đạt | Sau khi đạt |
|---|---|---|
| 1366 px | [Ảnh trước](enrollment-quiz-progress/quiz-progress-before-1366.png) | [Ảnh sau](enrollment-quiz-progress/quiz-progress-after-1366.png) |
| 768 px | [Ảnh trước](enrollment-quiz-progress/quiz-progress-before-768.png) | [Ảnh sau](enrollment-quiz-progress/quiz-progress-after-768.png) |
| 375 px | [Ảnh trước](enrollment-quiz-progress/quiz-progress-before-375.png) | [Ảnh sau](enrollment-quiz-progress/quiz-progress-after-375.png) |

## Tình huống đã kiểm

- ENROLL-13.1: S xong bài 1, đạt quiz bài 2 → bài 2 và khóa COMPLETED/100%, chứng chỉ có
  tên học viên, mỗi thông báo hoàn thành/cấp chứng chỉ đúng một lần.
- ENROLL-13.2/13.3: trượt hoặc quiz không gắn bài → quan sát nhiều lần, vẫn 50%.
- ENROLL-13.4: tác giả làm thử thành công nhưng không được tạo lượt ghi danh.
- ENROLL-13.5: bắt đầu quiz khi ACTIVE, hủy ghi danh rồi nộp đạt → vẫn CANCELLED/0%, không có chứng chỉ.
- ENROLL-13.8–13.12: kiểm snapshot thiếu/cũ/bài sai, bản sao sự kiện đồng thời, rollback giữa
  lúc lưu outbox, giữ completedAt/watchedSeconds, ghi danh cũ thiếu tên bằng transaction H2.
- Kafka nhúng kiểm retry, DLT giữ key/payload/topic/offset/group và sự kiện hợp lệ phía sau.
- ENROLL-13.13: Chromium thao tác mở quiz, gửi bài đạt qua API thật, bấm Back; trang học
  tự làm mới mà không cần bấm hoàn thành thủ công. Không có lỗi JavaScript hoặc tràn ngang.

## Kiểm hạ tầng của consumer quiz

Lượt đầu của probe ENROLL-13 bị **FAIL ở điều kiện đọc log**, vì Spring Boot rút tên luồng
`quiz-progress-0-C-1` còn 15 ký tự. Đây không phải kết quả PASS cho ca mất MySQL/replay.
Đã sửa runner ở `8302c773bf4f09462bc6601bd1f17737f7661a8d`; code nghiệp vụ không đổi.

Lượt xác nhận lại: [Resilience 37798977124](https://github.com/ariushieu/e-learning-microservices/actions/runs/37798977124).
**PASS cả ENROLL-13.6 và ENROLL-13.7**, chạy 22:20:02–22:20:51 UTC+7 trên MySQL 8.4/Kafka 4.3.1.
[Báo cáo gốc đã loại token](enrollment-quiz-progress/quiz-recovery.json) lấy từ
[artifact 11559759197](https://github.com/ariushieu/e-learning-microservices/actions/runs/37798977124/artifacts/11559759197).

| Kiểm tra | Quan sát thực tế |
|---|---|
| Source quiz | Partition 1, offset 0, eventId `3eea6482-26e2-491b-b0d3-4883b641f654` |
| MySQL tắt | 6 lần quan sát trong 12 giây đều chưa commit offset (`-1001` nghĩa là partition chưa có offset đã commit); 8 dòng lỗi DB của luồng quiz, không phải số lần retry |
| Sau khôi phục | 2 bài hoàn thành, khóa COMPLETED/100%, 1 ledger, 1 chứng chỉ, 1 `enrollment.completed`, 1 `certificate.issued` |
| Replay | Phát lại nguyên byte/key/eventId thêm 3 lần, offset cuối 3, consumer commit 4 |
| Sau replay | Mọi số đếm trên giữ nguyên, không tạo thêm chứng chỉ/outbox |

Workflow cũng chạy lại demo-flow, 5 ca ENROLL-09, smoke test sau khôi phục và dọn stack thành công.

## Giới hạn triển khai

- Phải triển khai course/quiz/enrollment/frontend cùng lượt; V4 chỉ thêm migration mới.
- Khóa cũ chưa có lessonIds cần chủ khóa/admin gọi PUT khóa với dữ liệu hiện tại để phát lại
  snapshot trước khi thử. Thiếu dữ liệu thì bỏ qua sự kiện, không đoán bài từ tổng số bài.
- Không xóa sổ eventId khi replay Kafka. Artifact CI có thời hạn lưu; các con số trên là
  kết quả của đúng commit và môi trường được ghi, không thay thế kiểm lại sau sửa đổi nghiệp vụ.
