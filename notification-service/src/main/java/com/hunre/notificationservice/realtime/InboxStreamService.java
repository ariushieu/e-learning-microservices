package com.hunre.notificationservice.realtime;

import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Luồng thông báo tức thời của một người dùng (Server-Sent Events).
 *
 * <p>Hai loại sự kiện, {@code data} đều là JSON:
 * <ul>
 *   <li>{@code unread-count} — {@code {"unreadCount": 3}}: gửi ngay khi mở luồng và mỗi khi số
 *       chưa đọc đổi, kể cả khi đọc ở tab khác.</li>
 *   <li>{@code notification} — một {@code NotificationResponse}, giống phần tử của
 *       {@code GET /api/notifications}.</li>
 * </ul>
 */
@Service
public class InboxStreamService {

    public static final String UNREAD_COUNT = "unread-count";
    public static final String NOTIFICATION = "notification";

    private static final Logger log = LoggerFactory.getLogger(InboxStreamService.class);

    private final SseHub hub;
    private final RealtimeBroadcaster broadcaster;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public InboxStreamService(SseHub hub, RealtimeBroadcaster broadcaster,
                              NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.hub = hub;
        this.broadcaster = broadcaster;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    /** Mở luồng và gửi ngay số chưa đọc, để trình duyệt nối lại là có số đúng. */
    public SseEmitter open(Long userId) {
        SseEmitter emitter = hub.open(userId);
        hub.send(userId, emitter, UNREAD_COUNT, unreadCountJson(userId));
        return emitter;
    }

    /**
     * Chạy sau khi transaction commit: phát trước khi commit thì trình duyệt có thể gọi lại API
     * mà chưa thấy thông báo, hoặc thấy một thông báo mà rollback sau đó xóa mất.
     * {@code fallbackExecution}: gọi ngoài transaction (test, chỗ gọi khác) thì vẫn phát.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onInboxChanged(InboxChanged change) {
        try {
            if (change.created() != null) {
                broadcaster.broadcast(change.userId(), NOTIFICATION,
                        objectMapper.writeValueAsString(change.created()));
            }
            broadcaster.broadcast(change.userId(), UNREAD_COUNT, unreadCountJson(change.userId()));
        } catch (RuntimeException e) {
            // Thông báo đã lưu xong. Lỗi ở đây mà ném ra thì Kafka xử lý lại sự kiện, gặp sổ
            // processed_events rồi bỏ qua — vô ích. Người dùng vẫn thấy nó khi tải lại.
            log.warn("Không phát được thay đổi hộp thư của người dùng {}: {}", change.userId(), e.getMessage());
        }
    }

    private String unreadCountJson(Long userId) {
        long count = notificationRepository.countByUserIdAndStatusNot(userId, NotificationStatus.READ);
        return objectMapper.writeValueAsString(Map.of("unreadCount", count));
    }
}
