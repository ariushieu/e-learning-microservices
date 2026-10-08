# Kiểm thử course-service qua Postman

Import **`course.postman_collection.json`**, chọn **No environment** rồi chạy cả collection bằng
Runner, một iteration, theo thứ tự đã lưu. Không cần import environment hay nhập ID/token.

Collection thực hiện 279 ca `COURSE-01.1`–`COURSE-28.29` trong
[course.md](../test-cases/course.md), cộng 16 ca hồi quy về sắp xếp, khóa lưu trữ và lọc danh mục cha/con.
Các thư mục đọc chạy trước các thư mục ghi để giữ fixture nền ổn định. Không sắp xếp lại các
request theo tên. Những thao tác có thể xóa/đổi dữ liệu dùng bản sao riêng.

## Chuẩn bị

- Chạy các service và MySQL/Kafka/Redis theo README dự án. Mọi request dùng gateway **8080**.
- `baseUrl` là collection variable, mặc định `http://localhost:8080`.
- Thư mục **0. Chuẩn bị** đăng nhập admin, đăng ký/đăng nhập tài khoản QA cố định theo
  [gateway.md](../test-cases/gateway.md), đặt lại quyền của A/B/S rồi đăng nhập lại.
- Tài khoản và mật khẩu mặc định chỉ dùng cho môi trường dev của nhóm. Nếu dùng môi trường khác,
  sửa `adminEmail`, `adminPassword`, `qaPassword` ở Collection → Variables.
- Đăng ký trả 409 chỉ xác nhận tài khoản đã tồn tại; request đăng nhập tiếp theo vẫn phải thành công.
- Token dùng các tên `adminToken`, `tokenA`, `tokenB`, `studentToken`; ID và token tự lưu vào
  **collection variables**, không dùng `pm.environment`.
- Không chạy đồng thời nhiều Runner trên cùng tài khoản QA. Nếu gặp 429 ở bước đăng nhập,
  chờ quota hồi phục rồi chạy lại từ đầu; chỉ tắt rate limit trên môi trường test theo hướng dẫn nhóm.

Fixture có `runId` riêng cho mỗi lần chạy. Collection chờ tối đa 30 giây cho snapshot/số học viên
qua Kafka. Thất bại chuẩn bị làm Runner dừng, không bỏ qua ca rồi coi là PASS. Không đổi kỳ vọng
trong file tình huống để làm xanh báo cáo.

## Kết quả

Mở **Test Results** để xem HTTP và kiểm tra nghiệp vụ. Ngoài HTTP, collection kiểm quyền sở hữu,
không thay đổi dữ liệu khi bị từ chối, slug, cây danh mục, nội dung/tài liệu bài học, thống kê
chương/bài, đồng bộ tên khóa vào snapshot, thứ tự sắp xếp và quyền học sau lưu trữ.

`COURSE-CATEGORY.1`–`.6` tạo cây hai cấp để kiểm danh mục cha gồm khóa trực tiếp và khóa con;
chọn danh mục con không lẫn khóa cha/anh em. Kiểm thêm lọc công khai, trình độ, từ khóa,
giảng viên, sắp xếp và tổng phân trang. Khóa nháp/lưu trữ không xuất hiện trong danh sách khách.

Ca bổ sung `COURSE-ARCHIVED.5` chấp nhận hai cách từ chối đang có trong enrollment-service:
snapshot ARCHIVED trả **422**, snapshot chưa cập nhật nhưng kiểm nguồn thấy khóa bị ẩn trả **404**.
Cả hai phải không tạo lượt ghi danh. Đây là ca bổ sung, không sửa kỳ vọng của 195 ca gốc.

`COURSE-25` có 25 ca đánh giá mới: tạo/sửa/xóa, quyền theo lịch sử ghi danh, lấy tên từ token,
không lộ email, giới hạn nhận xét, điểm nguyên 1–5, phân trang và hai kiểu ghi đồng thời.
Fixture S và A ghi danh qua API rồi chờ Kafka; không chèn quyền bằng SQL.

Dữ liệu được giữ lại cho demo và đối chiếu bằng chứng. Chạy lại tạo fixture mới; không tự xóa
khóa/danh mục đang có. Khóa từng có học viên phải lưu trữ, không xóa được bằng API theo quy tắc
đếm học viên lịch sử. Chỉ dọn những fixture có đúng `runId` của mình; không xóa trực tiếp DB dùng chung.
Không export collection đang chứa token thật để commit/chia sẻ.

Biên bản: [ket-qua/course.md](../test-cases/ket-qua/course.md). Collection cũ và environment của
course đã được thay thế; collection của service khác không thuộc thay đổi này.

## Tái tạo và chạy bằng dòng lệnh

JSON được sinh từ `scripts/build-course-collection.cjs`, dùng tên/mã ca từ tài liệu gốc. Sửa generator
rồi chạy lại, không sửa JSON một phía:

```bash
node scripts/build-course-collection.cjs
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json \
  --timeout-script 65000 --timeout-request 10000
```

Newman dùng Postman Runtime; kết quả của lần chạy này không có nghĩa là đã thao tác bằng giao diện
Postman Desktop. Báo cáo JSON đầy đủ của Newman chứa token từ request đăng nhập, chỉ lưu ở máy cá nhân.
Biên bản trong Git chỉ kèm bằng chứng đã loại dữ liệu xác thực.
