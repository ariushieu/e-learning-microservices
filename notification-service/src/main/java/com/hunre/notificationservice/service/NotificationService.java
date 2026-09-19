package com.hunre.notificationservice.service;

import com.hunre.notificationservice.dto.NotificationResponse;
import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.repository.NotificationPreferenceRepository;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.NotificationTemplateRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Dựng thông báo từ sự kiện và phục vụ hộp thư của người dùng.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final TemplateRenderer templateRenderer;

    /**
     * Tạo một thông báo trong ứng dụng từ mẫu tương ứng.
     *
     * <p>Trả về {@code empty} khi không tạo gì, vì hai lý do bình thường: người dùng đã tắt
     * thông báo trong ứng dụng, hoặc loại sự kiện này chưa có mẫu IN_APP. Cả hai đều không
     * phải lỗi nên không ném exception — consumer vẫn coi như đã xử lý xong sự kiện.
     *
     * @param code      mã mẫu, trùng {@code notification_templates.code}
     * @param userId    người nhận
     * @param variables giá trị điền vào các chỗ trống trong mẫu
     * @param linkUrl   đường dẫn mở khi bấm vào thông báo, có thể null
     */
    @Transactional
    public Optional<Notification> createInApp(String code, Long userId,
                                              Map<String, String> variables, String linkUrl) {

        if (!inAppEnabledFor(userId)) {
            log.debug("Người dùng {} đã tắt thông báo trong ứng dụng, bỏ qua {}", userId, code);
            return Optional.empty();
        }

        var template = templateRepository.findByCodeAndChannelAndActiveTrue(
                code, NotificationChannel.IN_APP);

        if (template.isEmpty()) {
            log.warn("Không có mẫu IN_APP đang bật cho mã {}, bỏ qua thông báo", code);
            return Optional.empty();
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(code)
                .channel(NotificationChannel.IN_APP)
                .title(templateRenderer.render(template.get().getTitleTemplate(), variables))
                .content(templateRenderer.render(template.get().getBodyTemplate(), variables))
                .linkUrl(linkUrl)
                // Thông báo trong ứng dụng coi như gửi xong ngay khi được lưu: người dùng
                // đọc trực tiếp từ database chứ không qua kênh nào khác.
                .status(NotificationStatus.SENT)
                .sentAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Đã tạo thông báo {} id={} cho người dùng {}", code, saved.getId(), userId);
        return Optional.of(saved);
    }

    public PageResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(
                page.getContent().stream().map(NotificationResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatusNot(userId, NotificationStatus.READ);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        // Tìm theo cả id lẫn userId, nếu không thì chỉ cần đoán id là đọc được thông báo
        // của người khác. Không tìm thấy thì trả 404 chứ không phải 403: người gọi không
        // cần biết thông báo đó có tồn tại hay không.
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("thông báo", "id", id));

        notification.markRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    private boolean inAppEnabledFor(Long userId) {
        // Không có bản ghi tùy chọn nghĩa là chưa tắt gì, tức là bật.
        return preferenceRepository.findById(userId)
                .map(pref -> Boolean.TRUE.equals(pref.getInAppEnabled()))
                .orElse(true);
    }
}
