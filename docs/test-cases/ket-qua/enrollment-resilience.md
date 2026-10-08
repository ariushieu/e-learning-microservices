# Biên bản demo-flow và ENROLL-09 — 08/10/2026

Phạm vi: kiểm tra luồng demo xuyên service và năm tình huống lỗi hạ tầng của enrollment.
Hướng dẫn chạy lại: [enrollment-resilience.md](../enrollment-resilience.md).

## Kiểm tra cục bộ

Backend đã ghép `main bb5023a`; PR này không sửa code ứng dụng, entity hoặc migration.
`mvn clean verify` bằng JDK 21.0.12, Maven 3.9.16 hoàn tất lúc 20:14:40 UTC+7:
**968 test đạt, 0 failure/error/skipped**. Các kiểm tra cú pháp Python, JavaScript và YAML đạt.

Đã kiểm riêng các điều kiện của runner: từ chối môi trường không phải project thử nghiệm;
report HTTP không xuất sentinel bí mật từ header/body/lỗi; nhận lỗi JDBC mức WARN/ERROR;
chọn đúng bản DLT theo consumer group; chờ metadata/leader của topic mới tạo.

## Docker/MySQL/Kafka

Commit kiểm: **`046f321631841840f52431d146eb5b0b8bc37e0a`**.
[Lượt Docker 37787839732](https://github.com/ariushieu/e-learning-microservices/actions/runs/37787839732)
đạt; cả **11 check của PR** tại commit này đều xanh. Smoke test qua gateway đạt cả trước khi gây
lỗi lẫn sau khi khôi phục. Stack thử nghiệm đã được dọn thành công.

**Demo-flow:** 20:57:29–20:57:48 UTC+7, **51 lượt HTTP, 81/81 assertion đạt**, không lỗi request.
Các lượt polling khiến số HTTP thực tế lớn hơn số request định nghĩa. Chuỗi ghi danh → tiến độ →
chứng chỉ/xác minh, quiz và thông báo đều qua gateway.

**ENROLL-09:** 20:58:01–21:00:22 UTC+7, **5/5 ca PASS**:

| Ca | Bằng chứng thực tế | Kết quả |
|---|---|---|
| 09.1 | MySQL enrollment dừng 41,2 giây; xuất bản qua API trả 200; 12 dòng lỗi kết nối từ listener; offset nguồn partition 2 chưa vượt 0 khi DB dừng; phục hồi thì snapshot đúng và committed offset = 1, không có DLT cho event đó | PASS |
| 09.2 | Tiêu đề 300 ký tự: source `0@2` → DLT `0@0`, giữ key/payload/header nguồn; message kế tiếp `0@3` tạo snapshot; committed offset = 4 | PASS |
| 09.3 | `not-json`: source `0@4` → DLT `2@0`; message kế tiếp `0@5` tạo snapshot; committed offset = 6 | PASS |
| 09.4 | Retry tạm 5 giây, DB giữ trạng thái dừng tới khi có DLT sau 11,1 giây; 8 dòng lỗi kết nối; source `0@7` → DLT `1@0`, committed offset = 8; DB lên thì message sau xử lý được; trả retry về 5 phút | PASS |
| 09.5 | Probe nhận `TOPIC_AUTHORIZATION_FAILED`; committed offset giữ 11 ở giây 0 / 3,4 / 6,7 / 10,1, bài kiểm tra snapshot sau vẫn chưa có. Gỡ ACL: source `0@11` → DLT `2@3` đúng group enrollment, message `0@12` tạo snapshot, committed offset = 13 | PASS |

Ký hiệu `partition@offset`. Committed offset là vị trí **message tiếp theo** sẽ đọc, nên giữ 11
nghĩa là chưa bỏ qua message lỗi ở offset 11. DLT có thể chọn partition khác source.
Ca 5 cũng quan sát bản DLT của `notification-service`; runner bỏ qua bản đó và chờ đúng bản của enrollment.

Bằng chứng đã kiểm và lưu cùng repository:

- [HTTP demo](enrollment-resilience/demo-http.json): tên request, mã HTTP, số assertion và lỗi.
- [Recovery](enrollment-resilience/recovery.json): các mẫu offset, event QA, bản DLT với key/payload,
  SHA-256 payload, header topic/partition/offset/group nguồn, snapshot sau phục hồi và kết quả cleanup.
- [Artifact CI gốc](https://github.com/ariushieu/e-learning-microservices/actions/runs/37787839732/artifacts/11555262267).

Không phát hiện lỗi nghiệp vụ trong lượt nghiệm thu này. Những thay đổi trong PR nằm ở runner,
workflow, overlay thử nghiệm và tài liệu; không sửa production để làm cho kiểm thử đạt.

## Giới hạn môi trường

- Workflow dùng sáu backend trên Docker, MySQL 8.4, Kafka 4.3.1, Redis, JWT bật.
  HTTP đều qua gateway; rate limit chỉ tắt tạm trong collection, bật lại trước các ca recovery.
- Enrollment dùng MySQL riêng trong overlay thử nghiệm. Course-service vẫn xuất bản được khóa
  trong lúc MySQL enrollment dừng; không chèn snapshot bằng SQL. Flyway dựng schema thật.
- Hikari chờ kết nối tối đa 1 giây. Ngân sách retry mặc định 5 phút, riêng ENROLL-09.4 đổi tạm
  thành 5 giây. Số dòng lỗi JDBC là bằng chứng lỗi kết nối, không coi là số lần thử chính xác.
- Ca 5 bật authorizer trong broker thử nghiệm và thêm ACL từ chối ghi DLT. Probe phải nhận
  `TOPIC_AUTHORIZATION_FAILED` trước khi bắt đầu; topic nguồn và việc đọc DLT vẫn hoạt động.
- Bản DLT của notification-service trên cùng topic không được tính thay cho enrollment.
  Key, payload, header nguồn và offset của enrollment được kiểm riêng.
- `finally` khôi phục database, retry và ACL; workflow dọn stack/volume thử nghiệm sau khi lưu
  bằng chứng. Không chạy các thao tác gây lỗi trên máy demo của nhóm.
- Workflow này không chạy giao diện; frontend và bộ Docker hiện có do workflow CI thông thường kiểm.
