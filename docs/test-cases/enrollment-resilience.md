# Chạy demo-flow và ENROLL-09 trên Docker

Bộ kiểm tra bổ sung cho [ENROLL-09.1–09.5](enrollment.md#enroll-09--retry-kafka-và-dlt).
Collection enrollment chỉ kiểm HTTP, không tự tắt database hoặc chặn DLT. Workflow
`Enrollment resilience` chạy các ca này riêng, bằng MySQL 8.4 và Kafka 4.3.1 thật.

## Phạm vi và môi trường

- Dùng host/runner thử nghiệm riêng. Không chạy trên máy demo hoặc stack đang có dữ liệu cần giữ.
- Overlay `infra/enrollment-resilience.compose.yml` cấp MySQL riêng cho enrollment. Dừng database
  này vẫn cho phép course-service xuất bản khóa qua gateway và gửi outbox lên Kafka. Tắt MySQL
  dùng chung trước khi xuất bản sẽ làm hỏng tiền điều kiện của ENROLL-09.1.
- Flyway dựng schema, Hibernate validate; JWT vẫn bật. Không ghi SQL vào `course_snapshots`.
- Chỉ giảm thời gian chờ lấy kết nối Hikari xuống 1 giây để giới hạn thời gian mỗi lần thử lỗi.
  ENROLL-09.1 dùng ngân sách retry mặc định 5 phút; riêng ENROLL-09.4 đổi thành 5 giây rồi trả về 5 phút.
- Mọi HTTP đi qua gateway 8080. Các ca message hỏng gửi trực tiếp vào Kafka để thử đúng đầu vào consumer.
- Script kiểm cờ môi trường, Compose project và DB_HOST trước khi dừng database.
- Broker thử nghiệm bật `StandardAuthorizer`, mặc định cho qua các resource chưa có ACL.
  Riêng ca 5 thêm ACL từ chối WRITE vào DLT; vẫn cho đọc DLT và ghi source topic. Script kiểm
  DLT chưa có ACL trước khi bắt đầu và chỉ xóa đúng những ACL do lượt kiểm tra tạo.

## Cách chạy trên Linux có Docker Compose

Các tên container cố định của compose gốc không cho chạy cạnh một stack elearning khác trên cùng
Docker host. Dùng runner mới như workflow, hoặc host thử nghiệm chưa chạy stack dự án.

```bash
export COMPOSE_PROJECT_NAME=enrollment-resilience
export COMPOSE_FILE=docker-compose.yml:infra/enrollment-resilience.compose.yml
export ENROLLMENT_RESILIENCE_DISPOSABLE=1
export SOURCE_COMMIT=$(git rev-parse HEAD)

docker compose --profile app build api-gateway auth-service course-service enrollment-service quiz-service notification-service
docker compose --profile app up -d --wait --wait-timeout 300 api-gateway
bash scripts/smoke-test.sh

# Chỉ tắt giới hạn khi chạy collection, luôn bật lại kể cả khi Newman lỗi.
(
  set -e
  trap 'RATE_LIMIT_ENABLED=true docker compose --profile app up -d --no-deps --wait api-gateway' EXIT
  RATE_LIMIT_ENABLED=false docker compose --profile app up -d --no-deps --wait api-gateway
  pnpm dlx newman@6.2.2 run docs/postman/demo-flow.postman_collection.json
)

python3 -m venv /tmp/enrollment-resilience-venv
/tmp/enrollment-resilience-venv/bin/pip install confluent-kafka==2.6.1
/tmp/enrollment-resilience-venv/bin/python scripts/check-enrollment-resilience.py
bash scripts/smoke-test.sh
```

## Điều kiện PASS

| Ca | Lỗi tạo thật | Bằng chứng bắt buộc |
|---|---|---|
| ENROLL-09.1 | Dừng MySQL enrollment ít nhất 40 giây, xuất bản khóa qua API trong lúc mất DB | Course event thực tế; ít nhất hai lỗi lấy kết nối trên listener; offset chưa vượt message lỗi; bật DB thì snapshot đúng, offset tiến lên và không có bản sao DLT |
| ENROLL-09.2 | Tiêu đề 300 ký tự | Key/payload gốc và header topic/partition/offset/group trong DLT; message kế tiếp liền kề trên cùng partition vẫn tạo snapshot; message lỗi không ghi DB |
| ENROLL-09.3 | Payload `not-json` | Cùng điều kiện DLT và xử lý message kế tiếp như ca 2 |
| ENROLL-09.4 | Retry 5 giây, MySQL dừng tới khi xuất hiện DLT | Có thử lại thật, quá ngân sách mới vào DLT; message sau xử lý được khi DB lên; trả cấu hình về 5 phút |
| ENROLL-09.5 | ACL từ chối WRITE vào DLT; probe phải nhận `TOPIC_AUTHORIZATION_FAILED` trước khi gửi source event | Quan sát một lần gửi DLT thực sự thất bại, tiếp tục kiểm ít nhất 5 giây sau đó: offset không bị bỏ qua và snapshot sau chưa được ghi; gỡ ACL thì DLT giữ payload/header và xử lý được message kế tiếp |

Không yêu cầu DLT đúng một bản: Kafka có thể gửi lại khi việc xác nhận/commit gặp lỗi. Điều bắt buộc
là không mất payload gốc và không vượt offset trước khi recovery thành công.
`notification-service` cũng nghe course events và dùng cùng DLT. Bằng chứng của enrollment phải có
header `kafka_dlt-original-consumer-group=enrollment-service`; bản của notification không được tính thay.

## Biên bản và dọn môi trường

`target/enrollment-resilience/recovery.json` lưu kết quả từng ca cùng offset, các course event QA
và header đã chọn. `demo-http.json` trong artifact chỉ có tên request, mã HTTP, số assertion và lỗi;
không chứa token, cookie, mật khẩu, response body hoặc report Newman thô.

Script dùng `finally` để bật lại MySQL, trả ngân sách retry và cấu hình DLT, kể cả khi ca thất bại.
Workflow luôn hủy stack thử nghiệm và volume sau khi upload bằng chứng. Nếu chạy thủ công, sau khi
đã sao lưu biên bản, dọn **project thử nghiệm ở trên** bằng:

```bash
docker compose --profile app down --volumes --remove-orphans
```

Chỉ ghi PASS sau khi có artifact của lượt chạy cụ thể. Test H2/embedded Kafka và việc thêm script
không thay thế kết quả chạy Docker. Biên bản lịch sử giữ nguyên; ghi kết quả mới vào file riêng.
