package com.hunre.sharedcommon.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("ok(data) luôn đánh dấu thành công và gắn thời điểm")
    void okVoiDuLieu() {
        Instant truoc = Instant.now();

        ApiResponse<String> response = ApiResponse.ok("xin chào");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("xin chào");
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isBetween(truoc, Instant.now());
    }

    @Test
    @DisplayName("ok(data, message) giữ cả dữ liệu lẫn thông báo")
    void okVoiThongBao() {
        ApiResponse<Integer> response = ApiResponse.ok(42, "Tạo khóa học thành công");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(42);
        assertThat(response.message()).isEqualTo("Tạo khóa học thành công");
    }

    @Test
    @DisplayName("message() dùng cho thao tác không có dữ liệu trả về")
    void chiCoThongBao() {
        ApiResponse<Void> response = ApiResponse.message("Đã xóa khóa học");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("Đã xóa khóa học");
    }
}
