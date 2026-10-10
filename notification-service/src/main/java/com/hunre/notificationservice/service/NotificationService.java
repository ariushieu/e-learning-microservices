package com.hunre.notificationservice.service;

import com.hunre.notificationservice.dto.NotificationPreferenceResponse;
import com.hunre.notificationservice.dto.NotificationResponse;
import com.hunre.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationPreference;
import com.hunre.notificationservice.entity.NotificationStatus;
import com.hunre.notificationservice.entity.UserContact;
import com.hunre.notificationservice.realtime.InboxChanged;
import com.hunre.notificationservice.repository.NotificationPreferenceRepository;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.NotificationTemplateRepository;
import com.hunre.notificationservice.repository.UserContactRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
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
    private final ApplicationEventPublisher events;
    private final UserContactRepository contactRepository;

    /** Địa chỉ web, để đổi đường dẫn tương đối của thông báo thành link bấm được trong email. */
    @Value("${elearning.web.base-url:http://localhost:3000}")
    private String webBaseUrl;

    /**
     * Gửi một thông báo qua mọi kênh có mẫu: tạo thông báo trong ứng dụng, và nếu mã này có mẫu
     * EMAIL thì xếp thêm một email vào hàng đợi. Mỗi kênh tự kiểm tra tùy chọn của người dùng.
     */
    @Transactional
    public Optional<Notification> deliver(String code, Long userId, Map<String, String> variables, String linkUrl) {
        Optional<Notification> inApp = createInApp(code, userId, variables, linkUrl);
        queueEmail(code, userId, variables, linkUrl);
        return inApp;
    }

    /**
     * Xếp một email vào hàng đợi (dòng notifications kênh EMAIL, trạng thái PENDING).
     * {@code EmailDispatcher} gửi sau khi commit, nên máy chủ mail chậm hay hỏng cũng không làm
     * việc xử lý sự kiện Kafka thất bại.
     *
     * <p>Trả về {@code empty} khi mã này không có mẫu EMAIL hoặc người dùng đã tắt email.
     */
    @Transactional
    public Optional<Notification> queueEmail(String code, Long userId, Map<String, String> variables, String linkUrl) {
        if (!emailEnabledFor(userId)) {
            return Optional.empty();
        }
        return enqueueEmail(code, userId, variables, linkUrl);
    }

    /**
     * Thư bảo mật do chính người dùng yêu cầu (đặt lại mật khẩu): gửi kể cả khi họ đã tắt email
     * thông báo, vì tắt email là để khỏi nhận tin tức chứ không phải để khóa luôn đường lấy lại tài khoản.
     */
    @Transactional
    public Optional<Notification> queueSecurityEmail(String code, Long userId, Map<String, String> variables, String linkUrl) {
        return enqueueEmail(code, userId, variables, linkUrl);
    }

    private Optional<Notification> enqueueEmail(String code, Long userId, Map<String, String> variables, String linkUrl) {
        var template = templateRepository.findByCodeAndChannelAndActiveTrue(code, NotificationChannel.EMAIL);
        if (template.isEmpty()) {
            return Optional.empty();
        }
        // Chép ra map mới: handler có thể dùng chung một map cho nhiều người nhận.
        Map<String, String> values = new HashMap<>(variables == null ? Map.of() : variables);
        values.put("fullName", contactRepository.findById(userId).map(UserContact::getFullName).orElse("bạn"));
        values.put("url", linkUrl == null ? webBaseUrl : webBaseUrl + linkUrl);

        Notification email = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(code)
                .channel(NotificationChannel.EMAIL)
                .title(templateRenderer.render(template.get().getTitleTemplate(), values))
                .content(templateRenderer.render(template.get().getBodyTemplate(), values))
                .linkUrl(linkUrl)
                .status(NotificationStatus.PENDING)
                .build());
        log.info("Đã xếp email {} id={} cho người dùng {}", code, email.getId(), userId);
        return Optional.of(email);
    }

    /** Ghi đè email và tên đã sao từ auth-service. */
    @Transactional
    public void saveContact(Long userId, String email, String fullName) {
        UserContact contact = contactRepository.findById(userId)
                .orElseGet(() -> UserContact.builder().userId(userId).build());
        contact.setEmail(email);
        contact.setFullName(fullName);
        contactRepository.save(contact);
    }

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
        events.publishEvent(InboxChanged.created(userId, NotificationResponse.from(saved)));
        return Optional.of(saved);
    }

    public PageResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP, pageable);
        return PageResponse.of(
                page.getContent().stream().map(NotificationResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndChannelAndStatusNot(userId, NotificationChannel.IN_APP, NotificationStatus.READ);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        // Tìm theo cả id lẫn userId, nếu không thì chỉ cần đoán id là đọc được thông báo
        // của người khác. Không tìm thấy thì trả 404 chứ không phải 403: người gọi không
        // cần biết thông báo đó có tồn tại hay không.
        Notification notification = notificationRepository.findByIdAndUserIdAndChannel(id, userId, NotificationChannel.IN_APP)
                .orElseThrow(() -> new ResourceNotFoundException("thông báo", "id", id));

        boolean wasUnread = notification.getStatus() != NotificationStatus.READ;
        notification.markRead();
        NotificationResponse response = NotificationResponse.from(notificationRepository.save(notification));
        if (wasUnread) {
            events.publishEvent(InboxChanged.read(userId));
        }
        return response;
    }

    /**
     * Đánh dấu đã đọc mọi thông báo của người dùng bằng một câu UPDATE.
     *
     * @return số thông báo vừa chuyển sang đã đọc; 0 nếu vốn không còn cái nào chưa đọc
     */
    @Transactional
    public int markAllRead(Long userId) {
        int updated = notificationRepository.markAllRead(userId, NotificationStatus.READ, Notification.now());
        if (updated > 0) {
            events.publishEvent(InboxChanged.read(userId));
        }
        return updated;
    }

    public NotificationPreferenceResponse getPreferences(Long userId) {
        return preferenceRepository.findById(userId)
                .map(NotificationPreferenceResponse::from)
                .orElse(NotificationPreferenceResponse.DEFAULTS);
    }

    /**
     * Ghi đè cả bộ tùy chọn. Chưa có bản ghi thì tạo — "không có bản ghi" vốn nghĩa là bật hết,
     * nên lần đầu tắt một kênh mới sinh ra dòng trong bảng.
     */
    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());
        preference.setInAppEnabled(request.inAppEnabled());
        preference.setEmailEnabled(request.emailEnabled());
        return NotificationPreferenceResponse.from(preferenceRepository.save(preference));
    }

    private boolean emailEnabledFor(Long userId) {
        return preferenceRepository.findById(userId)
                .map(pref -> Boolean.TRUE.equals(pref.getEmailEnabled()))
                .orElse(true);
    }

    private boolean inAppEnabledFor(Long userId) {
        // Không có bản ghi tùy chọn nghĩa là chưa tắt gì, tức là bật.
        return preferenceRepository.findById(userId)
                .map(pref -> Boolean.TRUE.equals(pref.getInAppEnabled()))
                .orElse(true);
    }
}
