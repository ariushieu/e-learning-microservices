package com.hunre.sharedcommon.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {

    @ParameterizedTest(name = "{0} phần tử, mỗi trang {1} -> {2} trang")
    @CsvSource({
            "0,  10, 0",
            "1,  10, 1",
            "10, 10, 1",
            "11, 10, 2",
            "19, 10, 2",
            "20, 10, 2",
            "21, 10, 3",
            "7,  3,  3"
    })
    @DisplayName("totalPages làm tròn lên, không bị hụt trang cuối")
    void tinhSoTrang(long totalElements, int size, int expectedTotalPages) {
        PageResponse<String> response = PageResponse.of(List.of(), 0, size, totalElements);

        assertThat(response.totalPages()).isEqualTo(expectedTotalPages);
    }

    @Test
    @DisplayName("trang đầu tiên có first = true")
    void trangDauTien() {
        PageResponse<String> response = PageResponse.of(List.of("a"), 0, 10, 25);

        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
    }

    @Test
    @DisplayName("trang cuối cùng có last = true")
    void trangCuoiCung() {
        PageResponse<String> response = PageResponse.of(List.of("a"), 2, 10, 25);

        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("chỉ có đúng một trang thì vừa là đầu vừa là cuối")
    void motTrangDuyNhat() {
        PageResponse<String> response = PageResponse.of(List.of("a"), 0, 10, 3);

        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("không có kết quả nào thì vẫn là trang đầu và trang cuối, không lỗi chia 0")
    void khongCoKetQua() {
        PageResponse<String> response = PageResponse.empty(0, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("content null được đổi thành danh sách rỗng, không để null lọt ra JSON")
    void contentNull() {
        PageResponse<String> response = PageResponse.of(null, 0, 10, 0);

        assertThat(response.content()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("content trả về không sửa được từ bên ngoài")
    void contentKhongSuaDuoc() {
        PageResponse<String> response = PageResponse.of(List.of("a"), 0, 10, 1);

        assertThatThrownBy(() -> response.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("size = 0 bị chặn thay vì ném lỗi chia cho 0 ở chỗ khác")
    void sizeKhongHopLe() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    @DisplayName("page âm bị chặn")
    void pageAm() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), -1, 10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    @DisplayName("totalElements âm bị chặn")
    void totalElementsAm() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalElements");
    }
}
