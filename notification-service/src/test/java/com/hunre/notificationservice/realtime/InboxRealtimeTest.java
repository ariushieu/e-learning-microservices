package com.hunre.notificationservice.realtime;

import com.hunre.notificationservice.dto.NotificationPreferenceResponse;
import com.hunre.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.entity.NotificationTemplate;
import com.hunre.notificationservice.repository.NotificationPreferenceRepository;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.NotificationTemplateRepository;
import com.hunre.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Hộp thư đổi thì luồng tức thời được báo — và chỉ sau khi dữ liệu đã thật sự lưu. Chạy trên
 * H2 thật vì thứ cần kiểm là thời điểm so với transaction, mock repository thì không có
 * transaction nào để so.
 */
@SpringBootTest
class InboxRealtimeTest {

    private static final long USER = 42L;

    @MockitoBean
    private RealtimeBroadcaster broadcaster;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SseHub hub;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        templateRepository.deleteAll();
        preferenceRepository.deleteAll();
        templateRepository.save(NotificationTemplate.builder()
                .code("QUIZ_GRADED")
                .channel(NotificationChannel.IN_APP)
                .titleTemplate("Đã có kết quả bài kiểm tra")
                .bodyTemplate("Bài kiểm tra {quizTitle} của bạn đạt {score} điểm.")
                .active(true)
                .build());
    }

    private void create() {
        notificationService.createInApp("QUIZ_GRADED", USER,
                Map.of("quizTitle", "Giữa kỳ", "score", "80.00"), "/attempts/1");
    }

    @Test
    @DisplayName("tạo thông báo thì phát thông báo mới rồi số chưa đọc")
    void taoThongBaoThiPhat() {
        transactionTemplate.executeWithoutResult(status -> create());

        var order = Mockito.inOrder(broadcaster);
        order.verify(broadcaster).broadcast(eq(USER), eq(InboxStreamService.NOTIFICATION), contains("\"linkUrl\":\"/attempts/1\""));
        order.verify(broadcaster).broadcast(USER, InboxStreamService.UNREAD_COUNT, "{\"unreadCount\":1}");
    }

    @Test
    @DisplayName("transaction rollback thì không phát gì: trình duyệt không thấy thông báo ma")
    void rollbackThiKhongPhat() {
        transactionTemplate.executeWithoutResult(status -> {
            create();
            status.setRollbackOnly();
        });

        assertThat(notificationRepository.count()).isZero();
        verify(broadcaster, never()).broadcast(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("đã tắt thông báo trong ứng dụng thì không tạo, không phát")
    void daTatThiKhongPhat() {
        notificationService.updatePreferences(USER, new UpdateNotificationPreferenceRequest(false, true));

        create();

        assertThat(notificationRepository.count()).isZero();
        verify(broadcaster, never()).broadcast(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("đọc tất cả: đổi mọi thông báo chưa đọc, giữ readAt cũ, báo số chưa đọc = 0")
    void docTatCa() {
        create();
        create();
        create();
        Notification first = notificationRepository.findAll().get(0);
        notificationService.markRead(first.getId(), USER);
        var firstReadAt = notificationRepository.findById(first.getId()).orElseThrow().getReadAt();
        Mockito.clearInvocations(broadcaster);

        int updated = notificationService.markAllRead(USER);

        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.findAll())
                .allSatisfy(n -> {
                    assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
                    assertThat(n.getReadAt()).isNotNull();
                });
        assertThat(notificationRepository.findById(first.getId()).orElseThrow().getReadAt())
                .isEqualTo(firstReadAt);
        verify(broadcaster).broadcast(USER, InboxStreamService.UNREAD_COUNT, "{\"unreadCount\":0}");
    }

    @Test
    @DisplayName("đọc tất cả khi không còn gì chưa đọc thì trả 0 và không phát")
    void docTatCaKhiDaDocHet() {
        assertThat(notificationService.markAllRead(USER)).isZero();
        verify(broadcaster, never()).broadcast(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("đọc tất cả không đụng hộp thư người khác")
    void docTatCaChiCuaMinh() {
        notificationService.createInApp("QUIZ_GRADED", 7L, Map.of("quizTitle", "A", "score", "1"), null);

        notificationService.markAllRead(USER);

        assertThat(notificationService.countUnread(7L)).isEqualTo(1);
    }

    @Test
    @DisplayName("đọc lại một thông báo đã đọc thì không phát lại số chưa đọc")
    void docLaiKhongPhat() {
        create();
        Long id = notificationRepository.findAll().get(0).getId();
        notificationService.markRead(id, USER);
        Mockito.clearInvocations(broadcaster);

        notificationService.markRead(id, USER);

        verify(broadcaster, never()).broadcast(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("chưa lưu tùy chọn thì coi như bật hết; lưu lần hai ghi đè lần một")
    void tuyChon() {
        assertThat(notificationService.getPreferences(USER))
                .isEqualTo(new NotificationPreferenceResponse(true, true));

        notificationService.updatePreferences(USER, new UpdateNotificationPreferenceRequest(false, false));
        notificationService.updatePreferences(USER, new UpdateNotificationPreferenceRequest(true, false));

        assertThat(notificationService.getPreferences(USER))
                .isEqualTo(new NotificationPreferenceResponse(true, false));
        assertThat(preferenceRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("một tài khoản mở quá số luồng cho phép thì luồng cũ nhất bị đóng")
    void gioiHanSoLuong() {
        for (int i = 0; i < 7; i++) {
            hub.open(99L);
        }

        assertThat(hub.openStreams(99L)).isEqualTo(5);
    }
}
