package com.hunre.othersvc;

import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.SortPropertyExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chứng minh {@code ?sort=} sai tên trường trả 400 chứ không phải 500.
 *
 * <p>Test chạy qua context thật thay vì {@code standaloneSetup} vì thứ dễ hỏng ở đây không
 * phải bản thân handler, mà là <b>thứ tự</b> giữa nó và lưới
 * {@code @ExceptionHandler(Exception.class)} của {@code GlobalExceptionHandler} — cả hai
 * đều được nạp bằng auto-configuration, đúng như khi chạy trong một service thật.
 *
 * <p>Thứ tự đó được kiểm riêng bằng {@link #uuTienCaoHonLuoiBatTat()} chứ không dựa vào
 * hai test gọi HTTP bên dưới: bỏ {@code @Order} đi thì hai advice cùng mức ưu tiên mặc
 * định, và thứ tự đăng ký bean hiện tại vẫn tình cờ cho ra 400 — đúng kiểu hỏng mà test
 * không thấy.
 *
 * <p>Gói {@code com.hunre.othersvc} nằm ngoài {@code com.hunre.sharedcommon} nên bean chỉ
 * có thể đến từ auto-configuration, không phải do component scan quét trúng.
 */
@SpringBootTest(
        classes = SortPropertyExceptionHandlerTest.TestApp.class,
        properties = "elearning.security.enabled=false")
@AutoConfigureMockMvc
class SortPropertyExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("service không khai báo gì vẫn có SortPropertyExceptionHandler")
    void handlerDuocDangKyTuDong() {
        assertThat(context.getBeansOfType(SortPropertyExceptionHandler.class))
                .as("bean phải đến từ auto-configuration của shared-common")
                .hasSize(1);
    }

    @Test
    @DisplayName("handler này phải được ưu tiên hơn lưới bắt tất của GlobalExceptionHandler")
    void uuTienCaoHonLuoiBatTat() {
        int uuTienRieng = OrderUtils.getOrder(SortPropertyExceptionHandler.class, Ordered.LOWEST_PRECEDENCE);
        int uuTienChung = OrderUtils.getOrder(GlobalExceptionHandler.class, Ordered.LOWEST_PRECEDENCE);

        assertThat(uuTienRieng)
                .as("""
                        Spring dừng ở advice đầu tiên có method khớp, mà GlobalExceptionHandler
                        bắt cả Exception. Handler cụ thể này phải xếp trước, nếu không
                        ?sort= sai tên trường quay lại thành 500.""")
                .isLessThan(uuTienChung);
    }

    @Test
    @DisplayName("sắp xếp theo trường không tồn tại trả 400 và nêu đúng tên trường")
    void sapXepTheoTruongKhongTonTai() throws Exception {
        mockMvc.perform(get("/test/sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Không sắp xếp được theo trường 'khongTonTai'"))
                .andExpect(jsonPath("$.path").value("/test/sort"));
    }

    @Test
    @DisplayName("thông báo nội bộ của Spring Data không lọt ra client")
    void khongLoChiTietNoiBo() throws Exception {
        String body = mockMvc.perform(get("/test/sort"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("tên lớp entity và gợi ý của Spring Data chỉ nên nằm trong log")
                .doesNotContain("PropertyReferenceException")
                .doesNotContain("KhoaHoc");
    }

    @SpringBootApplication
    static class TestApp {

        @RestController
        static class SortController {

            /** Mô phỏng đúng thứ Spring Data ném ra khi câu truy vấn phân trang chạy. */
            @GetMapping("/test/sort")
            String sort() {
                throw new PropertyReferenceException(
                        "khongTonTai",
                        TypeInformation.of(KhoaHoc.class),
                        List.<PropertyPath>of());
            }
        }

        record KhoaHoc(Long id, String tieuDe) {
        }
    }
}
