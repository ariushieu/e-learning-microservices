# Lớp học trực tuyến

Giảng viên lên lịch buổi học cho khóa, đến giờ mở phòng; học viên của khóa vào học bằng camera,
micro ngay trên web, như Google Meet. Có chia sẻ màn hình, chat, giơ tay, giảng viên tắt micro
học viên, và bảng điểm danh tự ghi thời gian có mặt.

## Ai làm gì

| Thành phần | Việc |
|---|---|
| course-service | Lịch học (`live_sessions`), điểm danh (`live_session_attendance`), phòng học trong bộ nhớ (`LiveRoomRegistry`), phát sự kiện `live.session.*` qua outbox |
| notification-service | Nhận `live.session.*`, tạo thông báo trong ứng dụng và email cho từng học viên |
| api-gateway | Chuyển `/api/live-sessions/**` tới course-service, kiểm JWT |
| frontend | Tab "Trực tuyến" trong trang quản lý khóa, mục "Lớp học trực tuyến" ở trang khóa, trang `/live/[id]` |

## Hình và tiếng đi đường nào

Máy chủ **không** chuyển hình và tiếng. Mỗi cặp người trong phòng có một kết nối WebRTC trực
tiếp giữa hai trình duyệt (kiểu "mesh"). Máy chủ chỉ chuyển các tin báo hiệu để hai trình duyệt
tìm thấy nhau:

```
Trình duyệt A ──POST /room/signal──► course-service ──SSE "signal"──► Trình duyệt B
            ◄═════════════ hình + tiếng (WebRTC, trực tiếp) ═════════════►
```

- Vào phòng: `GET /api/live-sessions/{id}/room` mở luồng Server-Sent Events. Sự kiện đầu tiên
  `welcome` có mã kết nối của mình và danh sách người đang trong phòng.
- Người mới vào gửi offer cho từng người có sẵn; người có sẵn chỉ trả lời, nên không bao giờ hai
  bên cùng gửi offer.
- ICE candidate được gom 60 ms một lần thành một request, để không vượt hạn mức 20 request/giây
  mỗi người của gateway.
- Bật/tắt camera, chia sẻ màn hình chỉ thay track trên kết nối có sẵn (`replaceTrack`), không
  phải thương lượng lại.

Mesh hợp với lớp nhỏ: mỗi người gửi hình cho mọi người khác, nên phòng giới hạn 12 người. Lớp
đông hơn cần máy chủ media (SFU) như LiveKit, Jitsi; khi đó chỉ thay phần WebRTC ở frontend,
lịch học, điểm danh, thông báo giữ nguyên.

## API

| Method | Đường dẫn | Ai | Việc |
|---|---|---|---|
| GET | `/api/courses/{courseId}/live-sessions` | Học viên của khóa, giảng viên, admin | Lịch học (50 buổi gần nhất) |
| POST | `/api/courses/{courseId}/live-sessions` | Giảng viên của khóa, admin | Lên lịch: `title`, `description`, `scheduledAt`, `durationMinutes` (15–300) |
| GET | `/api/live-sessions/{id}` | Như trên | Chi tiết một buổi, kèm số người đang trong phòng |
| POST | `/api/live-sessions/{id}/start` | Giảng viên | `SCHEDULED` → `LIVE`, báo học viên |
| POST | `/api/live-sessions/{id}/end` | Giảng viên | `LIVE` → `ENDED`, đóng phòng, chốt điểm danh |
| POST | `/api/live-sessions/{id}/cancel` | Giảng viên | `SCHEDULED` → `CANCELLED`, báo học viên |
| GET | `/api/live-sessions/{id}/attendance` | Giảng viên | Ai đã vào, vào lúc nào, có mặt bao lâu, vào mấy lần |
| GET | `/api/live-sessions/{id}/room?mic=&cam=` | Người được vào | Luồng SSE của phòng |
| POST | `/api/live-sessions/{id}/room/signal` | Người trong phòng | `{from, messages: [{to, type: offer\|answer\|candidate, data}]}` |
| POST | `/api/live-sessions/{id}/room/state` | Người trong phòng | `{from, mic, cam, hand, screen}` |
| POST | `/api/live-sessions/{id}/room/chat` | Người trong phòng | `{from, text}` (tối đa 1000 ký tự) |
| POST | `/api/live-sessions/{id}/room/mute` | Giảng viên trong phòng | `{from, target}`: nhờ một người tắt micro |
| POST | `/api/live-sessions/{id}/room/leave` | Người trong phòng | `{from}` |

`from` là mã kết nối nhận trong `welcome`; máy chủ kiểm mã đó đúng là của người gửi request.

Sự kiện SSE: `welcome`, `peer-joined`, `peer-updated`, `peer-left`, `signal`, `chat`,
`mute-request`, `ended`. Mỗi 15 giây có một dòng `: ping` giữ kết nối.

## Giới hạn

- Phòng nằm trong bộ nhớ của course-service: chạy nhiều bản course-service thì phải chuyển tin
  qua Redis như luồng thông báo của notification-service. Khởi động lại course-service thì mọi
  người tự nối lại (trình duyệt thử lại sau 2 giây).
- Chat không lưu vào database, chỉ giữ 100 tin gần nhất khi phòng còn mở.
- Camera, micro chỉ dùng được ở `https://` hoặc `localhost`. Demo trên một máy thì mở hai trình
  duyệt khác nhau (hoặc một cửa sổ ẩn danh) cho giảng viên và học viên.
- Hai máy khác mạng cần STUN (đang dùng `stun.l.google.com`); mạng chặn UDP thì cần thêm TURN.
