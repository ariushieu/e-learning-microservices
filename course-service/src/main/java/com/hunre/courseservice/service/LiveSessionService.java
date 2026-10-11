package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.ScheduleLiveSessionRequest;
import com.hunre.courseservice.dto.response.LiveAttendanceResponse;
import com.hunre.courseservice.dto.response.LiveSessionResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.entity.LiveSession;
import com.hunre.courseservice.entity.LiveSessionStatus;
import com.hunre.courseservice.event.CourseEventPublisher;
import com.hunre.courseservice.live.LiveRoomRegistry;
import com.hunre.courseservice.repository.CourseLearnerRepository;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.LiveAttendanceRepository;
import com.hunre.courseservice.repository.LiveSessionRepository;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.LiveSessionEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Buổi học trực tuyến: giảng viên lên lịch, bắt đầu, kết thúc hoặc hủy; học viên của khóa vào
 * phòng khi buổi học đang diễn ra. Mỗi lần lên lịch, bắt đầu, hủy đều phát sự kiện để
 * notification-service báo cho học viên (trên web và qua email).
 *
 * <p>Như thông báo của giảng viên, "học viên" là người có trong {@code course_learners}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveSessionService {
    /** Chấp nhận giờ bắt đầu lệch vài phút về quá khứ: đồng hồ máy người dùng chạy nhanh chậm khác nhau. */
    static final Duration PAST_TOLERANCE = Duration.ofMinutes(5);
    static final Duration MAX_AHEAD = Duration.ofDays(365);

    private final LiveSessionRepository sessions;
    private final LiveAttendanceRepository attendance;
    private final CourseRepository courses;
    private final CourseLearnerRepository learners;
    private final CourseEventPublisher publisher;
    private final LiveRoomRegistry rooms;

    public List<LiveSessionResponse> list(Long courseId, AuthenticatedUser user) {
        requireLogin(user);
        Course course = courses.findById(courseId).orElseThrow(() -> missingCourse(courseId));
        boolean manager = isManager(course, user);
        if (!manager && !learners.exists(courseId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ học viên của khóa mới xem được lịch học trực tuyến");
        }
        List<LiveSession> found = sessions.findTop50ByCourseIdOrderByScheduledAtDescIdDesc(courseId);
        Map<Long, Integer> counts = manager ? attendance.countLearners(found.stream().map(LiveSession::getId).toList()) : Map.of();
        return found.stream().map(s -> LiveSessionResponse.from(s, course.getTitle(), manager,
                counts.getOrDefault(s.getId(), 0), rooms.participantCount(s.getId()))).toList();
    }

    public LiveSessionResponse get(Long sessionId, AuthenticatedUser user) {
        LiveSession session = sessions.findById(sessionId).orElseThrow(() -> missingSession(sessionId));
        Course course = requireAccess(session, user);
        return response(session, course, user);
    }

    @Transactional
    public LiveSessionResponse schedule(Long courseId, ScheduleLiveSessionRequest request, AuthenticatedUser user) {
        Course course = requireManager(courseId, user);
        if (course.getStatus() == CourseStatus.DRAFT || course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Khóa học chưa xuất bản nên chưa có học viên để mở lớp trực tuyến");
        }
        Instant now = Instant.now();
        if (request.scheduledAt().isBefore(now.minus(PAST_TOLERANCE))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Thời gian bắt đầu đã qua, hãy chọn thời điểm khác");
        }
        if (request.scheduledAt().isAfter(now.plus(MAX_AHEAD))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ lên lịch trước tối đa một năm");
        }

        LiveSession session = new LiveSession();
        session.setCourseId(courseId);
        session.setHostId(user.userId());
        session.setHostName(blankToNull(user.fullName()));
        session.setTitle(request.title().strip());
        session.setDescription(blankToNull(request.description()));
        session.setScheduledAt(request.scheduledAt());
        session.setDurationMinutes(request.durationMinutes());
        sessions.saveAndFlush(session);
        notifyLearners(EventTypes.LIVE_SESSION_SCHEDULED, session, course);
        return response(session, course, user);
    }

    /** Mở phòng. Bấm lại khi đã mở thì trả về như cũ, không báo học viên lần nữa. */
    @Transactional
    public LiveSessionResponse start(Long sessionId, AuthenticatedUser user) {
        LiveSession session = sessions.findForUpdate(sessionId).orElseThrow(() -> missingSession(sessionId));
        Course course = requireManager(session.getCourseId(), user);
        if (session.getStatus() == LiveSessionStatus.SCHEDULED) {
            session.setStatus(LiveSessionStatus.LIVE);
            session.setStartedAt(Instant.now());
            notifyLearners(EventTypes.LIVE_SESSION_STARTED, session, course);
        } else if (session.getStatus() != LiveSessionStatus.LIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, session.getStatus() == LiveSessionStatus.ENDED
                    ? "Buổi học đã kết thúc, hãy lên lịch buổi mới" : "Buổi học đã bị hủy");
        }
        return response(session, course, user);
    }

    @Transactional
    public LiveSessionResponse end(Long sessionId, AuthenticatedUser user) {
        LiveSession session = sessions.findForUpdate(sessionId).orElseThrow(() -> missingSession(sessionId));
        Course course = requireManager(session.getCourseId(), user);
        if (session.getStatus() != LiveSessionStatus.LIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Chỉ kết thúc được buổi học đang diễn ra");
        }
        session.setStatus(LiveSessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        // Đóng phòng ngay trong giao dịch để thời gian có mặt cuối cùng được ghi cùng lúc với trạng thái
        // ENDED. Hiếm khi giao dịch lỗi sau bước này: mọi người chỉ cần vào lại phòng vẫn đang LIVE.
        rooms.close(sessionId);
        return response(session, course, user);
    }

    @Transactional
    public LiveSessionResponse cancel(Long sessionId, AuthenticatedUser user) {
        LiveSession session = sessions.findForUpdate(sessionId).orElseThrow(() -> missingSession(sessionId));
        Course course = requireManager(session.getCourseId(), user);
        if (session.getStatus() != LiveSessionStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Chỉ hủy được buổi học chưa bắt đầu");
        }
        session.setStatus(LiveSessionStatus.CANCELLED);
        notifyLearners(EventTypes.LIVE_SESSION_CANCELLED, session, course);
        return response(session, course, user);
    }

    public List<LiveAttendanceResponse> attendance(Long sessionId, AuthenticatedUser user) {
        LiveSession session = sessions.findById(sessionId).orElseThrow(() -> missingSession(sessionId));
        requireManager(session.getCourseId(), user);
        return attendance.findBySession(sessionId);
    }

    /**
     * Vào phòng. Không chạy trong transaction: luồng còn mở lâu sau khi hàm này trả về, và dòng
     * điểm danh phải tự commit (transaction chỉ-đọc của lớp này sẽ chặn câu INSERT).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter openRoom(Long sessionId, AuthenticatedUser user, boolean mic, boolean cam) {
        LiveSession session = sessions.findById(sessionId).orElseThrow(() -> missingSession(sessionId));
        Course course = requireAccess(session, user);
        switch (session.getStatus()) {
            case SCHEDULED -> throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Buổi học chưa bắt đầu");
            case ENDED -> throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Buổi học đã kết thúc");
            case CANCELLED -> throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Buổi học đã bị hủy");
            case LIVE -> { }
        }
        return rooms.join(sessionId, user, isManager(course, user), mic, cam);
    }

    private void notifyLearners(String eventType, LiveSession session, Course course) {
        List<Long> recipients = learners.findUserIds(course.getId());
        if (recipients.isEmpty()) return;
        publisher.publishLiveSessionEvent(LiveSessionEvent.of(eventType, session.getId(), course.getId(),
                course.getTitle(), session.getTitle(), session.getHostName(), session.getScheduledAt(),
                session.getDurationMinutes(), recipients));
    }

    private LiveSessionResponse response(LiveSession session, Course course, AuthenticatedUser user) {
        boolean manager = isManager(course, user);
        Integer attendees = manager
                ? attendance.countLearners(List.of(session.getId())).getOrDefault(session.getId(), 0) : null;
        return LiveSessionResponse.from(session, course.getTitle(), manager, attendees, rooms.participantCount(session.getId()));
    }

    private Course requireAccess(LiveSession session, AuthenticatedUser user) {
        requireLogin(user);
        Course course = courses.findById(session.getCourseId()).orElseThrow(() -> missingSession(session.getId()));
        if (!isManager(course, user) && !learners.exists(course.getId(), user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ học viên của khóa mới vào được lớp học trực tuyến");
        }
        return course;
    }

    private Course requireManager(Long courseId, AuthenticatedUser user) {
        requireLogin(user);
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên của khóa hoặc quản trị viên được quản lý lớp trực tuyến");
        }
        Course course = courses.findById(courseId).orElseThrow(() -> missingCourse(courseId));
        if (!isManager(course, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không phải giảng viên của khóa học này");
        }
        return course;
    }

    private static void requireLogin(AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        }
    }

    private static boolean isManager(Course course, AuthenticatedUser user) {
        return user.hasRole(Roles.ADMIN) || user.userId().equals(course.getInstructorId());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static ResourceNotFoundException missingCourse(Long id) {
        return new ResourceNotFoundException("khóa học", "id", id);
    }

    private static ResourceNotFoundException missingSession(Long id) {
        return new ResourceNotFoundException("buổi học trực tuyến", "id", id);
    }
}
