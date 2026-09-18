package com.hunre.sharedcommon.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.server.PathContainer.parsePath;

class PublicPathsTest {

    private boolean congKhai(PublicPaths paths, String method, String path) {
        return paths.matches(method, parsePath(path));
    }

    @Test
    @DisplayName("khai không có phương thức thì mọi phương thức đều công khai")
    void khongKhaiPhuongThuc() {
        PublicPaths paths = new PublicPaths(List.of("/api/auth/login"));

        assertThat(congKhai(paths, "POST", "/api/auth/login")).isTrue();
        assertThat(congKhai(paths, "GET", "/api/auth/login")).isTrue();
    }

    @Test
    @DisplayName("GET:/api/courses/** mở đọc nhưng KHÔNG mở ghi")
    void gioiHanTheoPhuongThuc() {
        PublicPaths paths = new PublicPaths(List.of("GET:/api/courses/**"));

        assertThat(congKhai(paths, "GET", "/api/courses")).isTrue();
        assertThat(congKhai(paths, "GET", "/api/courses/12/curriculum")).isTrue();

        // Đây là lý do tồn tại của tiền tố phương thức. Nếu ba dòng dưới thành true thì
        // bất kỳ ai cũng tạo, sửa và xóa được khóa học mà không cần đăng nhập.
        assertThat(congKhai(paths, "POST", "/api/courses")).isFalse();
        assertThat(congKhai(paths, "PUT", "/api/courses/12")).isFalse();
        assertThat(congKhai(paths, "DELETE", "/api/courses/12")).isFalse();
    }

    @Test
    @DisplayName("tên phương thức không phân biệt hoa thường")
    void khongPhanBietHoaThuong() {
        PublicPaths paths = new PublicPaths(List.of("get:/api/courses/**"));

        assertThat(congKhai(paths, "GET", "/api/courses")).isTrue();
    }

    @Test
    @DisplayName("đường dẫn không khai báo thì không công khai")
    void duongDanLa() {
        PublicPaths paths = new PublicPaths(List.of("GET:/api/courses/**", "/actuator/**"));

        assertThat(congKhai(paths, "GET", "/api/quizzes/1")).isFalse();
        assertThat(congKhai(paths, "GET", "/actuator/health")).isTrue();
    }

    @Test
    @DisplayName("/** chỉ khớp phần sau tiền tố, không khớp đường dẫn giống tên")
    void khongKhopNhamTienTo() {
        PublicPaths paths = new PublicPaths(List.of("GET:/api/courses/**"));

        // /api/courses-admin KHÔNG được coi là con của /api/courses
        assertThat(congKhai(paths, "GET", "/api/courses-admin")).isFalse();
    }

    @Test
    @DisplayName("danh sách rỗng hoặc null thì không có gì công khai")
    void danhSachRong() {
        assertThat(new PublicPaths(List.of()).isEmpty()).isTrue();
        assertThat(new PublicPaths(null).isEmpty()).isTrue();
        assertThat(congKhai(new PublicPaths(null), "GET", "/api/courses")).isFalse();
    }

    @Test
    @DisplayName("khoảng trắng thừa quanh mục khai báo không làm hỏng việc so khớp")
    void khoangTrangThua() {
        PublicPaths paths = new PublicPaths(List.of("  GET : /api/courses/**  ", "  "));

        assertThat(congKhai(paths, "GET", "/api/courses")).isTrue();
    }
}
