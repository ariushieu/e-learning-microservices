package com.hunre.sharedcommon.dto;

/**
 * Chi tiết lỗi của một trường dữ liệu, dùng trong {@link ErrorResponse} khi request
 * không vượt qua được bước kiểm tra ràng buộc.
 *
 * <p>Cố ý <b>không</b> có trường {@code rejectedValue}: giá trị bị từ chối có thể là mật
 * khẩu hoặc dữ liệu nhạy cảm, trả ngược về client và ghi vào log là rò rỉ không cần thiết.
 *
 * @param field   tên trường bị lỗi, ví dụ {@code email}
 * @param message mô tả lỗi để hiển thị cho người dùng
 */
public record FieldErrorDetail(String field, String message) {
}
