package com.hunre.courseservice.service.impl;

import com.hunre.courseservice.client.EnrollmentAccessClient;
import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
import com.hunre.courseservice.dto.request.UpdateSectionRequest;
import com.hunre.courseservice.dto.response.LessonResourceResponse;
import com.hunre.courseservice.dto.response.LessonResponse;
import com.hunre.courseservice.dto.response.SectionResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.Lesson;
import com.hunre.courseservice.entity.LessonResource;
import com.hunre.courseservice.entity.LessonType;
import com.hunre.courseservice.entity.Section;
import com.hunre.courseservice.event.CourseEventPublisher;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.LessonRepository;
import com.hunre.courseservice.repository.LessonResourceRepository;
import com.hunre.courseservice.repository.SectionRepository;
import com.hunre.courseservice.service.CurriculumService;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumServiceImpl implements CurriculumService {

    private final EnrollmentAccessClient enrollmentAccessClient;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CourseEventPublisher courseEventPublisher;

    @Override
    public List<SectionResponse> getCurriculumByCourseId(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));

        if (!canViewCourse(course)) {
            throw new ResourceNotFoundException("khóa học", "id", courseId);
        }

        List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAsc(courseId);
        boolean hasProtectedLessons = sections.stream().flatMap(section -> section.getLessons().stream())
                .anyMatch(lesson -> !Boolean.TRUE.equals(lesson.getIsPreview()));
        boolean fullAccess = false;
        try {
            // Khóa không công khai đã được kiểm quyền đầy đủ ở canViewCourse.
            fullAccess = hasProtectedLessons && (course.getStatus() != CourseStatus.PUBLISHED
                    || canReadProtectedContent(course));
        } catch (BusinessException exception) {
            if (exception.errorCode() != ErrorCode.EXTERNAL_SERVICE_ERROR) {
                throw exception;
            }
            log.warn("Không thể kiểm tra ghi danh cho khóa học {}; trả đề cương và ẩn nội dung bài thường",
                    courseId);
        }
        boolean includeProtectedContent = fullAccess;
        return sections.stream()
                .map(section -> SectionResponse.from(section,
                        lesson -> includeProtectedContent || Boolean.TRUE.equals(lesson.getIsPreview())))
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SectionResponse createSection(Long courseId, CreateSectionRequest request, Long currentUserId, boolean isAdmin) {
        Course course = lockCourse(courseId, currentUserId, isAdmin);

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle().trim())
                .position(request.getPosition() != null ? request.getPosition() : 0)
                .build();

        Section saved = sectionRepository.save(section);
        return SectionResponse.from(saved, lesson -> true);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SectionResponse updateSection(Long id, UpdateSectionRequest request, Long currentUserId, boolean isAdmin) {
        lockSectionCourse(id, currentUserId, isAdmin);
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", id));
        requireOwner(section.getCourse(), currentUserId, isAdmin);

        section.setTitle(request.getTitle().trim());
        section.setPosition(request.getPosition() != null ? request.getPosition() : 0);

        Section updated = sectionRepository.save(section);
        return SectionResponse.from(updated, lesson -> true);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteSection(Long id, Long currentUserId, boolean isAdmin) {
        lockSectionCourse(id, currentUserId, isAdmin);
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", id));
        requireOwner(section.getCourse(), currentUserId, isAdmin);

        Course course = section.getCourse();
        sectionRepository.delete(section);

        // Tính toán lại tổng số bài học và tổng thời lượng của khóa học
        updateCourseStats(course.getId());
    }

    @Override
    public LessonResponse getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));

        if (!canViewCourse(lesson.getCourse())) {
            throw new ResourceNotFoundException("bài học", "id", id);
        }

        return LessonResponse.from(lesson, lesson.getCourse().getStatus() != CourseStatus.PUBLISHED
                || Boolean.TRUE.equals(lesson.getIsPreview())
                || canReadProtectedContent(lesson.getCourse()));
    }

    private boolean canReadProtectedContent(Course course) {
        return currentUserProvider.getCurrentUser().map(user ->
                user.hasRole(Roles.ADMIN) || user.userId().equals(course.getInstructorId())
                        || enrollmentAccessClient.hasEnrollment(course.getId(), user.userId()))
                .orElse(false);
    }

    private boolean canViewCourse(Course course) {
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return true;
        }
        return currentUserProvider.getCurrentUser().map(user -> {
            if (user.hasRole(Roles.ADMIN) || user.userId().equals(course.getInstructorId())) {
                return true;
            }
            if (course.getStatus() != CourseStatus.ARCHIVED) {
                return false;
            }
            try {
                return enrollmentAccessClient.hasEnrollment(course.getId(), user.userId());
            } catch (BusinessException exception) {
                if (exception.errorCode() != ErrorCode.EXTERNAL_SERVICE_ERROR) {
                    throw exception;
                }
                log.warn("Không thể kiểm tra ghi danh cho khóa học lưu trữ {}; từ chối quyền đọc",
                        course.getId());
                return false;
            }
        }).orElse(false);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonResponse createLesson(Long sectionId, CreateLessonRequest request, Long currentUserId, boolean isAdmin) {
        lockSectionCourse(sectionId, currentUserId, isAdmin);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", sectionId));
        requireOwner(section.getCourse(), currentUserId, isAdmin);

        Course course = section.getCourse();
        int duration = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;

        Lesson lesson = Lesson.builder()
                .section(section)
                .course(course)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .contentUrl(request.getContentUrl())
                .type(request.getType() != null ? request.getType() : LessonType.VIDEO)
                .durationSeconds(duration)
                .position(request.getPosition() != null ? request.getPosition() : 0)
                .isPreview(Boolean.TRUE.equals(request.getIsPreview()))
                .build();

        Lesson saved = lessonRepository.save(lesson);

        // Cập nhật số liệu dẫn xuất của khóa học
        course.setTotalLessons(course.getTotalLessons() + 1);
        course.setTotalDurationSeconds(course.getTotalDurationSeconds() + duration);
        courseRepository.save(course);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            courseEventPublisher.publishCourseUpdated(course);
        }

        return LessonResponse.from(saved, true);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonResponse updateLesson(Long id, UpdateLessonRequest request, Long currentUserId, boolean isAdmin) {
        lockLessonCourse(id, currentUserId, isAdmin);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));
        requireOwner(lesson.getCourse(), currentUserId, isAdmin);

        int oldDuration = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;
        int newDuration = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;
        int durationDiff = newDuration - oldDuration;

        lesson.setTitle(request.getTitle().trim());
        lesson.setContent(request.getContent());
        lesson.setContentUrl(request.getContentUrl());
        lesson.setType(request.getType() != null ? request.getType() : LessonType.VIDEO);
        lesson.setDurationSeconds(newDuration);
        lesson.setPosition(request.getPosition() != null ? request.getPosition() : 0);
        lesson.setIsPreview(Boolean.TRUE.equals(request.getIsPreview()));

        Lesson updated = lessonRepository.save(lesson);

        if (durationDiff != 0) {
            Course course = updated.getCourse();
            course.setTotalDurationSeconds(Math.max(0, course.getTotalDurationSeconds() + durationDiff));
            courseRepository.save(course);
        }

        return LessonResponse.from(updated, true);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteLesson(Long id, Long currentUserId, boolean isAdmin) {
        lockLessonCourse(id, currentUserId, isAdmin);
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));
        requireOwner(lesson.getCourse(), currentUserId, isAdmin);

        Course course = lesson.getCourse();
        int duration = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;

        lessonRepository.delete(lesson);

        // Cập nhật lại số liệu dẫn xuất
        course.setTotalLessons(Math.max(0, course.getTotalLessons() - 1));
        course.setTotalDurationSeconds(Math.max(0, course.getTotalDurationSeconds() - duration));
        courseRepository.save(course);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            courseEventPublisher.publishCourseUpdated(course);
        }
    }

    @Override
    @Transactional
    public LessonResourceResponse addResource(Long lessonId, CreateLessonResourceRequest request, Long currentUserId, boolean isAdmin) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", lessonId));
        requireOwner(lesson.getCourse(), currentUserId, isAdmin);

        LessonResource resource = LessonResource.builder()
                .lesson(lesson)
                .name(request.getName().trim())
                .fileUrl(request.getFileUrl().trim())
                .build();

        LessonResource saved = lessonResourceRepository.save(resource);
        return LessonResourceResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteResource(Long lessonId, Long resourceId, Long currentUserId, boolean isAdmin) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("tài liệu bài học", "id", resourceId));
        if (!lessonId.equals(resource.getLesson().getId())) {
            throw new ResourceNotFoundException("tài liệu bài học", "id", resourceId);
        }
        requireOwner(resource.getLesson().getCourse(), currentUserId, isAdmin);

        lessonResourceRepository.delete(resource);
    }

    private void requireOwner(Course course, Long currentUserId, boolean isAdmin) {
        if (currentUserId == null || (!isAdmin && !currentUserId.equals(course.getInstructorId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền chỉnh sửa khóa học của giảng viên khác");
        }
    }

    private Course lockCourse(Long id, Long currentUserId, boolean isAdmin) {
        // Khóa cha trước khi đọc/sửa bài; READ_COMMITTED giúp lần đọc sau khi chờ khóa
        // thấy dữ liệu mới nhất, không giữ snapshot từ truy vấn tìm courseId trước đó.
        Course course = courseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));
        requireOwner(course, currentUserId, isAdmin);
        return course;
    }

    private void lockSectionCourse(Long id, Long currentUserId, boolean isAdmin) {
        Long courseId = sectionRepository.findCourseIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", id));
        lockCourse(courseId, currentUserId, isAdmin);
    }

    private void lockLessonCourse(Long id, Long currentUserId, boolean isAdmin) {
        Long courseId = lessonRepository.findCourseIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));
        lockCourse(courseId, currentUserId, isAdmin);
    }

    private void updateCourseStats(Long courseId) {
        courseRepository.findById(courseId).ifPresent(course -> {
            course.setTotalLessons(lessonRepository.countByCourseId(courseId));
            course.setTotalDurationSeconds(lessonRepository.sumDurationSecondsByCourseId(courseId));
            courseRepository.save(course);
            if (course.getStatus() == CourseStatus.PUBLISHED) {
                courseEventPublisher.publishCourseUpdated(course);
            }
        });
    }
}
