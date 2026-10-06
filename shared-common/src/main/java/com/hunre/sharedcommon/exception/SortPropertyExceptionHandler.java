package com.hunre.sharedcommon.exception;

import com.hunre.sharedcommon.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Trả 400 khi client gửi {@code ?sort=} theo một trường không tồn tại.
 *
 * <p><b>Vấn đề.</b> Mọi endpoint nhận {@code Pageable} đều để Spring Data tự dựng
 * {@code Sort} từ query param. Tên trường chỉ được đối chiếu với entity lúc câu truy vấn
 * chạy, tức là sâu trong tầng repository, nên lỗi nổ ra dưới dạng
 * {@link PropertyReferenceException} chứ không phải lỗi ràng buộc đầu vào:
 *
 * <pre>
 * GET /api/courses?sort=khongTonTai,desc
 * → 500 INTERNAL_ERROR
 * PropertyReferenceException: No property 'khongTonTai' found for type 'Course'
 * </pre>
 *
 * <p>Đó là lỗi của bên gọi (gõ sai tên trường) nhưng lại bị báo thành sự cố hệ thống.
 * Frontend không phân biệt được với lỗi thật, còn log thì đầy stack trace vô nghĩa.
 *
 * <p><b>Vì sao tách thành lớp riêng thay vì thêm một method vào
 * {@link GlobalExceptionHandler}.</b> {@code PropertyReferenceException} nằm trong
 * spring-data-commons, mà shared-common cố ý không phụ thuộc bắt buộc vào Spring Data
 * (xem {@code PageResponse}). Tách riêng thì chỉ lớp này cần thư viện đó, và
 * auto-configuration bọc nó bằng {@code @ConditionalOnClass}: service nào không dùng
 * Spring Data thì bean này không được tạo, không có {@code NoClassDefFoundError}.
 *
 * <p><b>Vì sao phải khai {@code @Order}.</b> {@link GlobalExceptionHandler} có
 * {@code @ExceptionHandler(Exception.class)} bắt tất. Spring duyệt các advice <b>theo thứ
 * tự ưu tiên</b> và dừng lại ở advice đầu tiên có method khớp — nó không so xem advice nào
 * khai kiểu exception cụ thể hơn. Để mặc định thì cả hai cùng
 * {@code LOWEST_PRECEDENCE}, thứ tự lúc đó do thứ tự đăng ký bean quyết định, tức là may
 * rủi. Thử đảo bằng cách cho {@code GlobalExceptionHandler} ưu tiên cao hơn thì response
 * đổi từ 400 sang 500 ngay, nên chỗ này phải khai rõ chứ không được dựa vào mặc định.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SortPropertyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SortPropertyExceptionHandler.class);

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleUnknownSortProperty(
            PropertyReferenceException ex, HttpServletRequest request) {

        log.warn("Tham số sắp xếp không hợp lệ tại {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.httpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST.name(),
                        "Không sắp xếp được theo trường '%s'".formatted(ex.getPropertyName()),
                        request.getRequestURI()));
    }
}
