package com.hunre.notificationservice.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Cấu hình luồng thông báo tức thời, đọc từ {@code elearning.realtime.*}.
 *
 * @param redisEnabled       {@code true}: phát qua Redis để mọi bản service cùng nhận;
 *                           {@code false}: chỉ phát cho kết nối nằm ở chính bản này
 * @param streamTimeout      luồng tự đóng sau khoảng này để trình duyệt nối lại bằng token
 *                           mới — luồng chỉ kiểm token lúc mở
 * @param heartbeatInterval  bao lâu gửi một dòng rỗng giữ kết nối
 * @param maxStreamsPerUser  số luồng tối đa của một tài khoản; vượt thì đóng luồng cũ nhất
 */
@ConfigurationProperties("elearning.realtime")
public record RealtimeProperties(
        @DefaultValue("true") boolean redisEnabled,
        @DefaultValue("15m") Duration streamTimeout,
        @DefaultValue("25s") Duration heartbeatInterval,
        @DefaultValue("5") int maxStreamsPerUser) {
}
