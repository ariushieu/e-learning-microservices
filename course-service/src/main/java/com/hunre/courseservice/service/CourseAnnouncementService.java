package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.PostAnnouncementRequest;
import com.hunre.courseservice.dto.response.CourseAnnouncementResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseAnnouncement;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.event.CourseEventPublisher;
import com.hunre.courseservice.repository.CourseAnnouncementRepository;
import com.hunre.courseservice.repository.CourseLearnerRepository;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.util.TextPreview;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.event.CourseAnnouncementPostedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giảng viên đăng thông báo cho học viên của khóa; notification-service nhận qua Kafka và
 * đưa vào hộp thư từng người.
 *
 * <p>Người nhận là những ai có trong {@code course_learners}, tức là từng ghi danh khóa này.
 * course-service không biết lượt ghi danh nào đã bị hủy, nên người đã hủy vẫn nhận thông báo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAnnouncementService {
    private final CourseAnnouncementRepository announcements;
    private final CourseRepository courses;
    private final CourseLearnerRepository learners;
    private final CourseEventPublisher publisher;

    public PageResponse<CourseAnnouncementResponse> list(Long courseId, int page, int size, AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để xem thông báo của khóa học");
        }
        Course course = courses.findById(courseId).orElseThrow(() -> missing(courseId));
        boolean manager = isManager(course, user);
        if (!manager && !learners.exists(courseId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ học viên của khóa mới xem được thông báo");
        }
        var result = announcements.findByCourseIdOrderByCreatedAtDescIdDesc(courseId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponse.of(result.getContent().stream().map(a -> CourseAnnouncementResponse.from(a, manager)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public CourseAnnouncementResponse post(Long courseId, PostAnnouncementRequest request, AuthenticatedUser user) {
        Course course = requireManager(courseId, user);
        if (course.getStatus() == CourseStatus.DRAFT || course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Khóa học chưa xuất bản nên chưa có học viên để gửi thông báo");
        }
        var recipients = learners.findUserIds(courseId);

        CourseAnnouncement announcement = new CourseAnnouncement();
        announcement.setCourseId(courseId);
        announcement.setAuthorId(user.userId());
        announcement.setAuthorName(user.fullName() == null || user.fullName().isBlank() ? null : user.fullName().strip());
        announcement.setTitle(request.title().strip());
        announcement.setContent(request.content().strip());
        announcement.setRecipientCount(recipients.size());
        announcements.saveAndFlush(announcement);

        if (!recipients.isEmpty()) {
            publisher.publishAnnouncementPosted(CourseAnnouncementPostedEvent.of(announcement.getId(), courseId,
                    course.getTitle(), announcement.getTitle(), TextPreview.of(announcement.getContent()), recipients));
        }
        return CourseAnnouncementResponse.from(announcement, true);
    }

    /** Chỉ gỡ khỏi trang khóa học; thông báo đã vào hộp thư học viên thì vẫn còn đó. */
    @Transactional
    public void delete(Long courseId, Long announcementId, AuthenticatedUser user) {
        requireManager(courseId, user);
        var announcement = announcements.findByIdAndCourseId(announcementId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("thông báo", "id", announcementId));
        announcements.delete(announcement);
    }

    private Course requireManager(Long courseId, AuthenticatedUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên của khóa hoặc quản trị viên được gửi thông báo");
        }
        Course course = courses.findById(courseId).orElseThrow(() -> missing(courseId));
        if (!isManager(course, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không phải giảng viên của khóa học này");
        }
        return course;
    }

    private static boolean isManager(Course course, AuthenticatedUser user) {
        return user.hasRole(Roles.ADMIN) || user.userId().equals(course.getInstructorId());
    }

    private static ResourceNotFoundException missing(Long id) {
        return new ResourceNotFoundException("khóa học", "id", id);
    }
}
