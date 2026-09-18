package com.hunre.sharedcommon.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra mọi loại lỗi đều ra đúng một hình dạng JSON và đúng mã trạng thái HTTP.
 * Đây là phần quan trọng nhất của shared-common vì frontend phụ thuộc trực tiếp vào nó.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException trả đúng status và mã lỗi của ErrorCode")
    void loiNghiepVu() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"))
                .andExpect(jsonPath("$.message").value("Khóa học chưa xuất bản"))
                .andExpect(jsonPath("$.path").value("/test/business"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ResourceNotFoundException trả 404 với thông báo dựng sẵn")
    void khongTimThay() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy khóa học với id = 99"));
    }

    @Test
    @DisplayName("DuplicateResourceException trả 409")
    void trungDuLieu() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("@Valid thất bại trả 400 kèm danh sách lỗi từng trường, sắp xếp theo tên")
    void loiKiemTraDuLieu() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"durationMinutes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("durationMinutes"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("tiêu đề không được để trống"));
    }

    @Test
    @DisplayName("ConstraintViolationException cũng trả về cùng hình dạng như @Valid")
    void loiRangBuocTangDuoi() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    @DisplayName("JSON hỏng trả 400 chứ không phải 500")
    void jsonHong() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{khong phai json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("tham số sai kiểu trả 400 và nêu đúng tên tham số")
    void thamSoSaiKieu() throws Exception {
        mockMvc.perform(get("/test/typed/khong-phai-so"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Tham số 'id' không đúng định dạng"));
    }

    @Test
    @DisplayName("exception chuẩn của Spring giữ nguyên status, không bị biến thành 500")
    void giuNguyenStatusCuaSpring() throws Exception {
        mockMvc.perform(get("/test/response-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("sai phương thức HTTP trả 405 với mã khớp đúng status")
    void saiPhuongThucHttp() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message")
                        .value("Phương thức HTTP không được hỗ trợ cho đường dẫn này"));
    }

    @Test
    @DisplayName("mã lỗi trả về luôn khớp với status đã khai báo trong ErrorCode")
    void maLoiKhopStatus() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.httpStatus())
                    .as("ErrorCode.%s phải có status riêng, không để null", code)
                    .isNotNull();
        }

        assertThat(ErrorCode.METHOD_NOT_ALLOWED.httpStatus().value()).isEqualTo(405);
        assertThat(ErrorCode.UNSUPPORTED_MEDIA_TYPE.httpStatus().value()).isEqualTo(415);
    }

    @Test
    @DisplayName("thông báo nội bộ tiếng Anh của Spring được thay bằng tiếng Việt")
    void khongLoThongBaoNoiBoCuaSpring() throws Exception {
        String body = mockMvc.perform(get("/test/response-status"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đường dẫn yêu cầu"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("chi tiết kỹ thuật của Spring không được lọt ra client")
                .doesNotContain("không có đường dẫn này")
                .doesNotContain("ResponseStatusException");
    }

    @Test
    @DisplayName("lỗi ngoài dự kiến trả 500 và KHÔNG lộ chi tiết nội bộ ra client")
    void khongLoChiTietNoiBo() throws Exception {
        String body = mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Hệ thống gặp sự cố, vui lòng thử lại sau"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("thông báo nội bộ không được lọt ra response")
                .doesNotContain("mat khau ket noi database")
                .doesNotContain("IllegalStateException");
    }

    // ---------------------------------------------------------------------

    record CreateCourseRequest(
            @NotBlank(message = "tiêu đề không được để trống") String title,
            @Min(value = 1, message = "thời lượng phải lớn hơn 0") int durationMinutes) {
    }

    record ConstrainedPayload(@NotBlank(message = "không được để trống") String title) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Khóa học chưa xuất bản");
        }

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("khóa học", "id", 99);
        }

        @GetMapping("/test/duplicate")
        String duplicate() {
            throw new DuplicateResourceException("người dùng", "email", "a@hunre.edu.vn");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody CreateCourseRequest request) {
            return request.title();
        }

        /** Mô phỏng ràng buộc bị vi phạm ở tầng dưới, ví dụ khi Hibernate lưu entity. */
        @GetMapping("/test/constraint")
        String constraint() {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                Validator validator = factory.getValidator();
                Set<ConstraintViolation<ConstrainedPayload>> violations =
                        validator.validate(new ConstrainedPayload(""));
                throw new ConstraintViolationException(violations);
            }
        }

        @GetMapping("/test/typed/{id}")
        String typed(@PathVariable Long id) {
            return String.valueOf(id);
        }

        @GetMapping("/test/response-status")
        String responseStatus() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "không có đường dẫn này");
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("mat khau ket noi database bi sai");
        }
    }
}
