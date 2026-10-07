package com.hunre.notificationservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Phát qua Redis pub/sub để mọi bản notification-service cùng nhận.
 *
 * <p>Vì sao cần: Kafka giao mỗi sự kiện cho <b>một</b> bản trong consumer group, còn trình
 * duyệt của người nhận có thể đang nối vào một bản khác. Bản tạo thông báo đăng lên kênh
 * Redis; bản nào đang giữ luồng của người đó thì gửi xuống, bản khác bỏ qua. Bản đăng cũng
 * nhận lại tin của chính nó, nên luồng nằm cùng bản cũng chỉ được gửi một lần.
 *
 * <p>Redis không lưu tin pub/sub: bản nào không nghe lúc đó thì mất. Chấp nhận được, vì
 * thông báo đã nằm trong MySQL và trình duyệt nối lại thì nhận ngay số chưa đọc mới.
 */
public class RedisRealtimeBroadcaster implements RealtimeBroadcaster, MessageListener {

    static final String CHANNEL = "elearning:notifications";

    private static final Logger log = LoggerFactory.getLogger(RedisRealtimeBroadcaster.class);

    private final StringRedisTemplate redis;
    private final SseHub hub;
    private final ObjectMapper objectMapper;

    public RedisRealtimeBroadcaster(StringRedisTemplate redis, SseHub hub, ObjectMapper objectMapper) {
        this.redis = redis;
        this.hub = hub;
        this.objectMapper = objectMapper;
    }

    @Override
    public void broadcast(Long userId, String event, String json) {
        String message = objectMapper.writeValueAsString(new RealtimeMessage(userId, event, json));
        try {
            redis.convertAndSend(CHANNEL, message);
        } catch (DataAccessException e) {
            // Redis chết: ít nhất người đang nối vào chính bản này vẫn nhận được.
            log.warn("Không đăng được lên Redis, chỉ gửi cho kết nối tại bản này: {}", e.getMessage());
            hub.send(userId, event, json);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RealtimeMessage received = objectMapper.readValue(message.getBody(), RealtimeMessage.class);
            hub.send(received.userId(), received.event(), received.data());
        } catch (JacksonException e) {
            log.warn("Bỏ qua tin Redis không đọc được trên kênh {}: {}", CHANNEL, e.getMessage());
        }
    }

    /** Hình dạng tin trên kênh Redis. */
    record RealtimeMessage(Long userId, String event, String data) {
    }
}
