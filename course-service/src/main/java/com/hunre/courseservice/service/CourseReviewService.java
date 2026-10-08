package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.SaveCourseReviewRequest;
import com.hunre.courseservice.dto.request.SaveReviewReplyRequest;
import com.hunre.courseservice.dto.response.CourseReviewResponse;
import com.hunre.courseservice.dto.response.MyCourseReviewResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseReview;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.*;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.*;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {
    private final CourseReviewRepository reviews;
    private final CourseRepository courses;
    private final CourseLearnerRepository learners;
    private final CourseService courseService;

    public PageResponse<CourseReviewResponse> list(Long courseId, Pageable pageable) {
        // Dùng cùng quyền xem khóa học, tránh lộ đánh giá của khóa nháp qua URL công khai.
        courseService.getCourseById(courseId);
        for (var order : pageable.getSort()) {
            if (!order.getProperty().equals("createdAt") && !order.getProperty().equals("id")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Đánh giá chỉ hỗ trợ sắp xếp theo thời gian tạo và ID");
            }
        }
        var ordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var page = reviews.findByCourseId(courseId, ordered);
        return PageResponse.of(page.getContent().stream().map(CourseReviewResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public MyCourseReviewResponse mine(Long courseId, AuthenticatedUser user) {
        requireRole(user);
        Course course = courses.findById(courseId).orElseThrow(() -> missing(courseId));
        if (!reviewable(course) && !user.hasRole(Roles.ADMIN) && !user.userId().equals(course.getInstructorId())) {
            throw missing(courseId);
        }
        boolean eligible = reviewable(course) && learners.exists(courseId, user.userId());
        if (course.getStatus() == CourseStatus.ARCHIVED && !eligible
                && !user.hasRole(Roles.ADMIN) && !user.userId().equals(course.getInstructorId())) throw missing(courseId);
        return new MyCourseReviewResponse(eligible, reviews.findByCourseIdAndUserId(courseId, user.userId())
                .map(CourseReviewResponse::from).orElse(null));
    }

    @Transactional
    public CourseReviewResponse save(Long courseId, SaveCourseReviewRequest request, AuthenticatedUser user) {
        requireLearnerAndLock(courseId, user);
        var review = reviews.findByCourseIdAndUserId(courseId, user.userId()).orElseGet(CourseReview::new);
        review.setCourseId(courseId);
        review.setUserId(user.userId());
        review.setRating(request.rating().byteValueExact());
        review.setComment(request.comment() == null ? null : request.comment().trim());
        review.setAuthorName(user.fullName() == null || user.fullName().isBlank() ? "Học viên" : user.fullName().trim());
        reviews.saveAndFlush(review);
        courses.recalculateRating(courseId);
        return CourseReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long courseId, AuthenticatedUser user) {
        requireLearnerAndLock(courseId, user);
        var review = reviews.findByCourseIdAndUserId(courseId, user.userId())
                .orElseThrow(() -> new ResourceNotFoundException("đánh giá", "courseId", courseId));
        deleteAndRecalculate(courseId, review);
    }

    @Transactional
    public void removeByAdmin(Long courseId, Long reviewId, AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        }
        if (!user.hasRole(Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản trị viên mới được gỡ đánh giá của người khác");
        }
        // Cùng thứ tự khóa với ghi/sửa/tự xóa để số liệu luôn khớp khi thao tác đồng thời.
        courses.lockForLearnerUpdate(courseId).orElseThrow(() -> missing(courseId));
        var review = reviews.findByIdAndCourseId(reviewId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("đánh giá", "id", reviewId));
        deleteAndRecalculate(courseId, review);
    }

    private void deleteAndRecalculate(Long courseId, CourseReview review) {
        reviews.delete(review);
        reviews.flush();
        courses.recalculateRating(courseId);
    }

    @Transactional
    public CourseReviewResponse saveReply(Long courseId, Long reviewId, SaveReviewReplyRequest request,
                                          AuthenticatedUser user) {
        var review = requireReplyManagerAndLock(courseId, reviewId, user);
        review.setReply(request.content().strip());
        review.setRepliedAt(java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        review.setRepliedBy(user.userId());
        // Phản hồi không thay đổi số sao, số lượt hay thứ tự ngày tạo của đánh giá.
        return CourseReviewResponse.from(reviews.saveAndFlush(review));
    }

    @Transactional
    public void deleteReply(Long courseId, Long reviewId, AuthenticatedUser user) {
        var review = requireReplyManagerAndLock(courseId, reviewId, user);
        if (review.getReply() == null) throw new ResourceNotFoundException("phản hồi", "reviewId", reviewId);
        review.setReply(null);
        review.setRepliedAt(null);
        review.setRepliedBy(null);
        reviews.saveAndFlush(review);
    }

    private CourseReview requireReplyManagerAndLock(Long courseId, Long reviewId, AuthenticatedUser user) {
        requireRole(user);
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên của khóa hoặc quản trị viên được phản hồi");
        }
        // Khóa trước khi đọc: không ghi đè sửa sao hoặc làm sống lại đánh giá vừa bị xóa.
        courses.lockForLearnerUpdate(courseId).orElseThrow(() -> missing(courseId));
        var course = courses.findById(courseId).orElseThrow(() -> missing(courseId));
        if (!user.hasRole(Roles.ADMIN) && !user.userId().equals(course.getInstructorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không phải giảng viên của khóa học này");
        }
        return reviews.findByIdAndCourseId(reviewId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("đánh giá", "id", reviewId));
    }

    private void requireLearnerAndLock(Long courseId, AuthenticatedUser user) {
        requireRole(user);
        // Khóa trước mọi lần đọc để snapshot và số liệu không bị cũ khi ghi đồng thời.
        courses.lockForLearnerUpdate(courseId).orElseThrow(() -> missing(courseId));
        Course course = courses.findById(courseId).orElseThrow(() -> missing(courseId));
        if (!reviewable(course)) throw missing(courseId);
        if (!learners.exists(courseId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ học viên đã ghi danh mới được đánh giá khóa học");
        }
    }

    private boolean reviewable(Course course) {
        return course.getStatus() == CourseStatus.PUBLISHED || course.getStatus() == CourseStatus.ARCHIVED;
    }

    private void requireRole(AuthenticatedUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        if (!user.hasAnyRole(Roles.STUDENT, Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản không có quyền đánh giá khóa học");
        }
    }

    private ResourceNotFoundException missing(Long id) {
        return new ResourceNotFoundException("khóa học", "id", id);
    }
}
