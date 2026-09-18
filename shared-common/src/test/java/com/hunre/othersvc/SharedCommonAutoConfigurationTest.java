package com.hunre.othersvc;

import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chứng minh {@link GlobalExceptionHandler} được đăng ký tự động ở một service bất kỳ.
 *
 * <p>Gói của test này là {@code com.hunre.othersvc}, cố ý nằm <b>ngoài</b>
 * {@code com.hunre.sharedcommon}. Ứng dụng test bên dưới chỉ quét component từ
 * {@code com.hunre.othersvc} trở xuống, đúng như auth-service chỉ quét
 * {@code com.hunre.authservice}. Vì vậy nếu bean vẫn có mặt thì nó chỉ có thể đến từ
 * cơ chế auto-configuration, không phải do component scan vô tình quét trúng.
 *
 * <p>Đây là thứ dễ hỏng âm thầm nhất: quên file
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * thì mọi thứ vẫn biên dịch và chạy bình thường, chỉ có điều lỗi trả về sai định dạng
 * mà không ai nhận ra cho tới khi frontend gọi thử.
 */
@SpringBootTest(
        classes = SharedCommonAutoConfigurationTest.TestApp.class,
        // Test nay kiem tra auto-config cua phan xu ly loi, khong lien quan xac thuc.
        // Khong tat thi context khong khoi dong duoc vi thieu elearning.security.jwt-secret.
        properties = "elearning.security.enabled=false")
class SharedCommonAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("service không khai báo gì vẫn có GlobalExceptionHandler")
    void handlerDuocDangKyTuDong() {
        assertThat(context.getBeansOfType(GlobalExceptionHandler.class))
                .as("bean phải đến từ auto-configuration của shared-common")
                .hasSize(1);
    }

    @SpringBootApplication
    static class TestApp {
    }
}
