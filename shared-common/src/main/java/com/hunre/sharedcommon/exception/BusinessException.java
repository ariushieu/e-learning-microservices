package com.hunre.sharedcommon.exception;

/**
 * Lỗi nghiệp vụ đã lường trước, ném ra từ tầng service.
 *
 * <p>{@code GlobalExceptionHandler} bắt exception này và tự dựng response lỗi với đúng
 * mã trạng thái HTTP lấy từ {@link ErrorCode}, nên controller không cần try/catch.
 *
 * <p>Thông báo truyền vào sẽ hiển thị thẳng cho người dùng, vì vậy hãy viết bằng tiếng
 * Việt và đừng nhét chi tiết kỹ thuật như tên bảng, câu SQL hay stack trace vào đó.
 *
 * <pre>{@code
 * if (course.getStatus() != CourseStatus.PUBLISHED) {
 *     throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
 *             "Khóa học chưa được xuất bản nên không thể ghi danh");
 * }
 * }</pre>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.INTERNAL_ERROR : errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode == null ? ErrorCode.INTERNAL_ERROR : errorCode;
    }

    /** Lỗi vi phạm quy tắc nghiệp vụ, mã mặc định {@link ErrorCode#BUSINESS_RULE_VIOLATED}. */
    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_RULE_VIOLATED, message);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
