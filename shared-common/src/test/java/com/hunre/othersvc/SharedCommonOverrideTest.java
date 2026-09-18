package com.hunre.othersvc;

import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service tự khai báo handler riêng thì bản mặc định của shared-common phải nhường chỗ,
 * nhờ {@code @ConditionalOnMissingBean}. Không có điều này thì hai handler cùng tồn tại
 * và Spring sẽ chọn một cách khó đoán.
 */
@SpringBootTest(classes = SharedCommonOverrideTest.TestApp.class)
class SharedCommonOverrideTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("handler riêng của service thay thế được bản mặc định")
    void serviceGhiDeDuoc() {
        assertThat(context.getBeansOfType(GlobalExceptionHandler.class))
                .as("chỉ được có đúng một handler")
                .hasSize(1);

        assertThat(context.getBean(GlobalExceptionHandler.class))
                .isInstanceOf(CustomHandler.class);
    }

    /** Mô phỏng handler riêng mà một service có thể tự viết. */
    static class CustomHandler extends GlobalExceptionHandler {
    }

    @SpringBootApplication
    static class TestApp {

        @Bean
        GlobalExceptionHandler customExceptionHandler() {
            return new CustomHandler();
        }
    }
}
