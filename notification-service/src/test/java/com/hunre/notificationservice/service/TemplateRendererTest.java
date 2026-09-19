package com.hunre.notificationservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    @DisplayName("điền đúng giá trị vào từng chỗ trống")
    void dienGiaTri() {
        String result = renderer.render(
                "Bài kiểm tra {quizTitle} của bạn đạt {score} điểm.",
                Map.of("quizTitle", "Chương 1", "score", "85.50"));

        assertThat(result).isEqualTo("Bài kiểm tra Chương 1 của bạn đạt 85.50 điểm.");
    }

    @Test
    @DisplayName("một biến xuất hiện nhiều lần đều được thay")
    void bienLapLai() {
        assertThat(renderer.render("{a} rồi {a}", Map.of("a", "x")))
                .isEqualTo("x rồi x");
    }

    @Test
    @DisplayName("thiếu giá trị thì thay bằng rỗng chứ không để lộ {tenBien} cho người dùng")
    void thieuGiaTri() {
        String result = renderer.render("Chào {fullName}, bạn đã ghi danh {courseTitle}.",
                Map.of("courseTitle", "Microservices"));

        assertThat(result).isEqualTo("Chào , bạn đã ghi danh Microservices.");
        assertThat(result).doesNotContain("{fullName}");
    }

    @Test
    @DisplayName("giá trị chứa $ hoặc \\ không làm hỏng việc thay thế")
    void kyTuDacBiet() {
        Map<String, String> vars = new HashMap<>();
        vars.put("price", "100$");
        vars.put("path", "C:\\khoa-hoc");

        assertThat(renderer.render("{price} tại {path}", vars))
                .isEqualTo("100$ tại C:\\khoa-hoc");
    }

    @Test
    @DisplayName("mẫu không có chỗ trống nào thì giữ nguyên")
    void khongCoChoTrong() {
        assertThat(renderer.render("Chúc mừng bạn!", Map.of()))
                .isEqualTo("Chúc mừng bạn!");
    }

    @Test
    @DisplayName("ngoặc nhọn không phải tên biến thì để nguyên")
    void ngoacNhonThuong() {
        assertThat(renderer.render("Công thức {1 + 2} và {}", Map.of()))
                .isEqualTo("Công thức {1 + 2} và {}");
    }

    @Test
    @DisplayName("mẫu null hoặc rỗng trả về chuỗi rỗng, không ném lỗi")
    void mauRong() {
        assertThat(renderer.render(null, Map.of())).isEmpty();
        assertThat(renderer.render("", Map.of())).isEmpty();
    }

    @Test
    @DisplayName("map biến null vẫn chạy được")
    void bienNull() {
        assertThat(renderer.render("Chào {ten}", null)).isEqualTo("Chào ");
    }
}
