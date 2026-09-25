package com.hunre.courseservice.service.impl;

import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.request.UpdateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.Category;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CategoryRepository;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.service.CourseService;
import com.hunre.courseservice.util.SlugUtils;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.sharedcommon.security.Roles;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hunre.courseservice.event.CourseEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CourseEventPublisher courseEventPublisher;

    @Override
    public PageResponse<CourseSummaryResponse> getPublishedCourses(
            Long categoryId, CourseLevel level, String keyword, Pageable pageable) {

        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy các khóa học đã được xuất bản (PUBLISHED)
            predicates.add(cb.equal(root.get("status"), CourseStatus.PUBLISHED));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate summaryMatch = cb.like(cb.lower(root.get("summary")), pattern);
                predicates.add(cb.or(titleMatch, summaryMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Course> page = courseRepository.findAll(spec, pageable);
        List<CourseSummaryResponse> content = page.getContent().stream()
                .map(CourseSummaryResponse::from)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResponse<CourseSummaryResponse> getInstructorCourses(Long instructorId, Pageable pageable) {
        boolean canSeeAll = currentUserProvider.getCurrentUser()
                .filter(u -> u.hasRole(Roles.ADMIN) || u.userId().equals(instructorId))
                .isPresent();

        Page<Course> page = canSeeAll
                ? courseRepository.findByInstructorId(instructorId, pageable)
                : courseRepository.findByInstructorIdAndStatus(instructorId, CourseStatus.PUBLISHED, pageable);

        List<CourseSummaryResponse> content = page.getContent().stream()
                .map(CourseSummaryResponse::from)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));

        if (!canViewCourse(course)) {
            throw new ResourceNotFoundException("khóa học", "id", id);
        }

        return CourseResponse.from(course);
    }

    @Override
    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "slug", slug));

        if (!canViewCourse(course)) {
            throw new ResourceNotFoundException("khóa học", "slug", slug);
        }

        return CourseResponse.from(course);
    }

    private boolean canViewCourse(Course course) {
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return true;
        }
        return currentUserProvider.getCurrentUser()
                .filter(u -> u.hasRole(Roles.ADMIN) || u.userId().equals(course.getInstructorId()))
                .isPresent();
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request, Long instructorId, String instructorName) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "id", request.getCategoryId()));

        String slug = resolveSlug(request.getTitle(), request.getSlug());

        if (courseRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("khóa học", "slug", slug);
        }

        String resolvedInstructorName = request.getInstructorName() != null && !request.getInstructorName().isBlank()
                ? request.getInstructorName().trim()
                : (instructorName != null && !instructorName.isBlank() ? instructorName.trim() : null);

        Course course = Course.builder()
                .category(category)
                .instructorId(instructorId)
                .instructorName(resolvedInstructorName)
                .title(request.getTitle().trim())
                .slug(slug)
                .summary(request.getSummary() != null ? request.getSummary().trim() : null)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .thumbnailUrl(request.getThumbnailUrl() != null ? request.getThumbnailUrl().trim() : null)
                .level(request.getLevel() != null ? request.getLevel() : CourseLevel.BEGINNER)
                .language(request.getLanguage() != null && !request.getLanguage().isBlank() ? request.getLanguage().trim() : "vi")
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .status(CourseStatus.DRAFT)
                .totalLessons(0)
                .totalDurationSeconds(0)
                .studentCount(0)
                .ratingAvg(BigDecimal.ZERO)
                .ratingCount(0)
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request, Long currentUserId, boolean isAdmin) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));

        if (!isAdmin && currentUserId != null && !course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền chỉnh sửa khóa học của giảng viên khác");
        }

        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể chỉnh sửa khóa học đã lưu trữ (ARCHIVED)");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "id", request.getCategoryId()));

        String slug = resolveSlug(request.getTitle(), request.getSlug());

        if (courseRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateResourceException("khóa học", "slug", slug);
        }

        course.setCategory(category);
        course.setInstructorName(request.getInstructorName() != null ? request.getInstructorName().trim() : null);
        course.setTitle(request.getTitle().trim());
        course.setSlug(slug);
        course.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
        course.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        course.setThumbnailUrl(request.getThumbnailUrl() != null ? request.getThumbnailUrl().trim() : null);
        course.setLevel(request.getLevel() != null ? request.getLevel() : CourseLevel.BEGINNER);
        course.setLanguage(request.getLanguage() != null && !request.getLanguage().isBlank() ? request.getLanguage().trim() : "vi");
        course.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);

        Course updated = courseRepository.save(course);
        Course savedCourse = updated != null ? updated : course;

        if (savedCourse.getStatus() == CourseStatus.PUBLISHED) {
            courseEventPublisher.publishCourseUpdated(savedCourse);
        }

        return CourseResponse.from(savedCourse);
    }

    @Override
    @Transactional
    public CourseResponse changeCourseStatus(Long id, ChangeCourseStatusRequest request, Long currentUserId, boolean isAdmin) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));

        if (!isAdmin && currentUserId != null && !course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền thay đổi trạng thái khóa học của giảng viên khác");
        }

        CourseStatus oldStatus = course.getStatus();
        CourseStatus newStatus = request.getStatus();

        // Ghi nhận thời điểm xuất bản lần đầu
        if (newStatus == CourseStatus.PUBLISHED && course.getPublishedAt() == null) {
            course.setPublishedAt(Instant.now());
        }

        course.setStatus(newStatus);
        Course updated = courseRepository.save(course);
        Course savedCourse = updated != null ? updated : course;

        // Phát sự kiện nếu khóa học chuyển sang PUBLISHED hoặc từ PUBLISHED sang trạng thái khác
        if (newStatus == CourseStatus.PUBLISHED || oldStatus == CourseStatus.PUBLISHED) {
            courseEventPublisher.publishCourseUpdated(savedCourse);
        }

        return CourseResponse.from(savedCourse);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id, Long currentUserId, boolean isAdmin) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));

        if (!isAdmin && currentUserId != null && !course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền xóa khóa học của giảng viên khác");
        }

        // Chỉ được xóa khóa học ở trạng thái DRAFT
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Chỉ được phép xóa khóa học ở trạng thái bản nháp (DRAFT)");
        }

        if (course.getStudentCount() != null && course.getStudentCount() > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể xóa khóa học đã có học viên đăng ký");
        }

        courseRepository.delete(course);
    }

    private String resolveSlug(String title, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return SlugUtils.toSlug(providedSlug);
        }
        return SlugUtils.toSlug(title);
    }
}
