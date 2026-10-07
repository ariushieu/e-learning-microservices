package com.hunre.notificationservice.realtime;

import com.hunre.notificationservice.controller.NotificationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis chưa lên lúc khởi động thì service vẫn phải lên: mất Redis chỉ được phép mất phần
 * tức thời giữa các bản, không được mất cả hộp thư. Cổng 1 không có gì nghe, kết nối bị từ
 * chối ngay — giống job CI "Schema matches entities" chạy service mà không có Redis.
 */
@SpringBootTest(properties = {
        "elearning.realtime.redis-enabled=true",
        "spring.data.redis.port=1"
})
class RedisUnavailableTest {

    @Autowired
    private NotificationController controller;

    @Autowired
    private RedisSubscription subscription;

    @Test
    @DisplayName("Redis chưa lên: context vẫn khởi động, đăng ký kênh chờ thử lại")
    void khoiDongDuocKhiChuaCoRedis() {
        assertThat(controller).isNotNull();
        assertThat(subscription.isRunning()).isTrue();
        assertThat(subscription.isSubscribed()).isFalse();
    }
}
