package com.hunre.sharedcommon.exception;

import org.springframework.http.HttpStatus;

/**
 * Bộ mã lỗi dùng chung cho toàn hệ thống.
 *
 * <p>Mỗi mã gắn với một mã trạng thái HTTP cố định, để cùng một loại lỗi ở service nào
 * cũng trả về cùng một status. Frontend xử lý theo tên mã (ví dụ {@code RESOURCE_NOT_FOUND})
 * chứ không so sánh chuỗi thông báo, vì thông báo có thể đổi bất cứ lúc nào.
 *
 * <p>Thêm mã mới thì thêm vào đây, đừng tự chế mã riêng trong service.
 */
public enum ErrorCode {

    /** Dữ liệu gửi lên không qua được ràng buộc kiểm tra. */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),

    /** Request sai định dạng: thiếu tham số, sai kiểu dữ liệu, JSON hỏng. */
    BAD_REQUEST(HttpStatus.BAD_REQUEST),

    /** Chưa đăng nhập hoặc token không hợp lệ, đã hết hạn. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

    /** Đã đăng nhập nhưng không đủ quyền thực hiện thao tác. */
    FORBIDDEN(HttpStatus.FORBIDDEN),

    /** Không tìm thấy tài nguyên được yêu cầu. */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** Gọi đúng đường dẫn nhưng sai phương thức HTTP. */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),

    /** Tài nguyên đã tồn tại: email trùng, slug trùng, ghi danh hai lần. */
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),

    /** Content-Type gửi lên không được hỗ trợ. */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    /** Request hợp lệ về hình thức nhưng vi phạm quy tắc nghiệp vụ. */
    BUSINESS_RULE_VIOLATED(HttpStatus.UNPROCESSABLE_ENTITY),

    /** Gọi sang service khác bị lỗi hoặc không phản hồi. */
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),

    /** Lỗi ngoài dự kiến, không lộ chi tiết ra ngoài. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
