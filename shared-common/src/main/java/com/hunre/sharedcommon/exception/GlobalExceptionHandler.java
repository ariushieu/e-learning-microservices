package com.hunre.sharedcommon.exception;

import com.hunre.sharedcommon.dto.ErrorResponse;
import com.hunre.sharedcommon.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;

/**
 * Chuyển mọi exception thành response lỗi cùng một hình dạng ({@link ErrorResponse})
 * cho cả 5 service.
 *
 * <p>Lớp này được đăng ký tự động qua auto-configuration của Spring Boot, service
 * <b>không</b> cần khai báo {@code @ComponentScan} hay tạo bean thủ công. Xem
 * {@code com.hunre.sharedcommon.autoconfigure.SharedCommonAutoConfiguration}.
 *
 * <p>Service muốn xử lý riêng một loại exception thì tự khai báo
 * {@code @RestControllerAdvice} trong service đó, và <b>phải kèm {@code @Order}</b> với mức
 * ưu tiên cao hơn. Spring dừng ở advice đầu tiên có method khớp chứ không chọn advice khai
 * kiểu exception cụ thể hơn, mà lớp này có {@code @ExceptionHandler(Exception.class)} bắt
 * tất — quên {@code @Order} thì handler riêng bị lớp này nuốt mất. Xem
 * {@link SortPropertyExceptionHandler} làm mẫu.
 *
 * <p>Lưu ý: trong file này {@code ErrorResponse} là DTO của dự án, còn interface
 * {@code org.springframework.web.ErrorResponse} của Spring được viết đầy đủ tên gói
 * ở chỗ duy nhất dùng tới, để tránh nhầm hai thứ trùng tên.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Lỗi nghiệp vụ đã lường trước. Ghi log mức WARN vì đây không phải sự cố hệ thống. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Lỗi nghiệp vụ [{}] tại {}: {}",
                ex.errorCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(ex.errorCode().httpStatus())
                .body(ErrorResponse.of(
                        ex.errorCode().name(), ex.getMessage(), request.getRequestURI()));
    }

    /** Dữ liệu trong body không qua được {@code @Valid}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), messageOf(error)))
                .sorted(Comparator.comparing(FieldErrorDetail::field))
                .toList();

        return validationFailed(fieldErrors, request);
    }

    /** Ràng buộc trên tham số của method, ví dụ {@code @Min} trên biến đường dẫn. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(
                        lastNodeOf(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .sorted(Comparator.comparing(FieldErrorDetail::field))
                .toList();

        return validationFailed(fieldErrors, request);
    }

    /** JSON gửi lên sai cú pháp hoặc sai kiểu dữ liệu. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Body không đọc được tại {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.name(),
                        "Dữ liệu gửi lên không đọc được, kiểm tra lại định dạng JSON",
                        request.getRequestURI()));
    }

    /** Tham số sai kiểu, ví dụ gọi {@code /api/courses/abc} khi id là số. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.name(),
                        "Tham số '%s' không đúng định dạng".formatted(ex.getName()),
                        request.getRequestURI()));
    }

    /**
     * Lưới cuối cùng cho mọi exception còn lại.
     *
     * <p>Các exception chuẩn của Spring MVC (không tìm thấy đường dẫn, sai HTTP method,
     * sai kiểu nội dung...) đều cài {@code org.springframework.web.ErrorResponse} và đã
     * mang sẵn mã trạng thái đúng. Phải giữ nguyên mã đó, nếu không một request gọi sai
     * đường dẫn sẽ bị báo thành 500 và che mất lỗi thật.
     *
     * <p>Lỗi ngoài dự kiến thì ghi log đầy đủ ở phía server nhưng chỉ trả ra client một
     * thông báo chung, tránh lộ cấu trúc nội bộ hay câu SQL.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();

            // Thông báo gốc của Spring là tiếng Anh và lộ chi tiết khung ứng dụng
            // ("No static resource api/... for request ..."), không phù hợp để hiển thị
            // cho người dùng. Giữ lại trong log, trả ra ngoài thông báo tiếng Việt.
            log.warn("Request không hợp lệ tại {} ({}): {}",
                    request.getRequestURI(), status, ex.getMessage());

            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(
                            codeFor(status), messageFor(status), request.getRequestURI()));
        }

        log.error("Lỗi ngoài dự kiến tại {}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "Hệ thống gặp sự cố, vui lòng thử lại sau",
                        request.getRequestURI()));
    }

    private ResponseEntity<ErrorResponse> validationFailed(
            List<FieldErrorDetail> fieldErrors, HttpServletRequest request) {

        log.warn("Dữ liệu không hợp lệ tại {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Dữ liệu gửi lên không hợp lệ",
                        request.getRequestURI(),
                        fieldErrors));
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "không hợp lệ" : error.getDefaultMessage();
    }

    /** {@code createCourse.title} chỉ giữ lại {@code title} cho gọn. */
    private static String lastNodeOf(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }

    /**
     * Thông báo tiếng Việt cho các exception chuẩn của Spring MVC.
     *
     * <p>Muốn hiển thị thông báo riêng cho một tình huống nghiệp vụ thì ném
     * {@link BusinessException}, đừng dùng {@code ResponseStatusException}: thông báo
     * truyền vào đó sẽ bị thay bằng nội dung chung ở đây.
     */
    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Yêu cầu không hợp lệ";
            case 401 -> "Bạn cần đăng nhập để thực hiện thao tác này";
            case 403 -> "Bạn không có quyền thực hiện thao tác này";
            case 404 -> "Không tìm thấy đường dẫn yêu cầu";
            case 405 -> "Phương thức HTTP không được hỗ trợ cho đường dẫn này";
            case 415 -> "Kiểu dữ liệu gửi lên không được hỗ trợ";
            default -> status.is4xxClientError()
                    ? "Yêu cầu không hợp lệ"
                    : "Hệ thống gặp sự cố, vui lòng thử lại sau";
        };
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ErrorCode.BAD_REQUEST.name();
            case 401 -> ErrorCode.UNAUTHORIZED.name();
            case 403 -> ErrorCode.FORBIDDEN.name();
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND.name();
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED.name();
            case 409 -> ErrorCode.DUPLICATE_RESOURCE.name();
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE.name();
            default -> status.is4xxClientError()
                    ? ErrorCode.BAD_REQUEST.name()
                    : ErrorCode.INTERNAL_ERROR.name();
        };
    }
}
