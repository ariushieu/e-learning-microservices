package com.hunre.notificationservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Các luồng SSE đang mở tới <b>chính bản service này</b>, nhóm theo người dùng.
 *
 * <p>Chỉ biết kết nối cục bộ. Thông báo tạo ở bản khác đến được đây là nhờ
 * {@link RealtimeBroadcaster} chuyển qua Redis rồi gọi {@link #send}.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);

    private final Map<Long, Deque<SseEmitter>> streams = new ConcurrentHashMap<>();
    private final RealtimeProperties properties;

    public SseHub(RealtimeProperties properties) {
        this.properties = properties;
    }

    /** Mở một luồng mới cho người dùng; quá số luồng cho phép thì đóng luồng cũ nhất. */
    public SseEmitter open(Long userId) {
        SseEmitter emitter = new SseEmitter(properties.streamTimeout().toMillis());
        List<SseEmitter> evicted = new ArrayList<>();

        // compute giữ khóa của đúng khóa userId: thêm luồng và gỡ luồng (ở remove) không xen
        // vào nhau được, nên không có chuyện thêm vào một hàng đợi vừa bị gỡ khỏi map.
        streams.compute(userId, (id, userStreams) -> {
            Deque<SseEmitter> target = userStreams == null ? new ConcurrentLinkedDeque<>() : userStreams;
            target.addLast(emitter);
            while (target.size() > properties.maxStreamsPerUser()) {
                evicted.add(target.pollFirst());
            }
            return target;
        });
        evicted.forEach(SseEmitter::complete);

        emitter.onCompletion(() -> remove(userId, emitter));
        // Hết giờ thì đóng êm. Không đăng ký thì Spring ném AsyncRequestTimeoutException và
        // GlobalExceptionHandler cố viết JSON lỗi lên một response đang là event-stream.
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> remove(userId, emitter));
        return emitter;
    }

    /** Gửi một sự kiện tới mọi luồng của người dùng đang nối vào bản này. */
    public void send(Long userId, String event, String json) {
        Deque<SseEmitter> userStreams = streams.get(userId);
        if (userStreams == null) {
            return;
        }
        for (SseEmitter emitter : userStreams) {
            sendTo(userId, emitter, SseEmitter.event().name(event).data(json));
        }
    }

    /** Gửi riêng cho một luồng, dùng cho sự kiện đầu tiên ngay khi vừa mở. */
    public void send(Long userId, SseEmitter emitter, String event, String json) {
        sendTo(userId, emitter, SseEmitter.event().name(event).data(json));
    }

    /**
     * Dòng chú thích không phải sự kiện, trình duyệt bỏ qua. Có hai việc: proxy ở giữa không cắt
     * luồng vì im lặng lâu, và ghi thất bại cho biết tab đã đóng để gỡ luồng đó ra.
     */
    @Scheduled(fixedDelayString = "${elearning.realtime.heartbeat-interval:25s}")
    public void heartbeat() {
        streams.forEach((userId, userStreams) ->
                userStreams.forEach(emitter -> sendTo(userId, emitter, SseEmitter.event().comment("ping"))));
    }

    /** Số luồng đang mở của một người, cho test và log. */
    public int openStreams(Long userId) {
        Deque<SseEmitter> userStreams = streams.get(userId);
        return userStreams == null ? 0 : userStreams.size();
    }

    private void sendTo(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // Trình duyệt đã đóng kết nối, hoặc luồng vừa đóng xong. Container tự kết thúc
            // response; ở đây chỉ cần không gửi vào đó nữa.
            log.debug("Gỡ luồng SSE của người dùng {}: {}", userId, e.getMessage());
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        streams.computeIfPresent(userId, (id, userStreams) -> {
            userStreams.remove(emitter);
            return userStreams.isEmpty() ? null : userStreams;
        });
    }
}
