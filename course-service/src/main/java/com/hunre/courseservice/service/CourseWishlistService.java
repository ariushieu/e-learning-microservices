package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.CourseWishlistRepository;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Khóa học yêu thích của từng người. Chỉ lưu được khóa đang xuất bản; khóa đã lưu mà sau đó bị
 * gỡ xuất bản thì vẫn nằm trong bảng nhưng không hiện ra, xuất bản lại là hiện lại.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseWishlistService {
    private final CourseWishlistRepository wishlist;
    private final CourseRepository courses;

    public List<CourseSummaryResponse> list(AuthenticatedUser user) {
        List<Long> ids = wishlist.findCourseIds(requireUser(user));
        Map<Long, Course> byId = courses.findAllById(ids).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return ids.stream().map(byId::get)
                .filter(c -> c != null && c.getStatus() == CourseStatus.PUBLISHED)
                .map(CourseSummaryResponse::from).toList();
    }

    public List<Long> ids(AuthenticatedUser user) {
        return wishlist.findCourseIds(requireUser(user));
    }

    @Transactional
    public void add(Long courseId, AuthenticatedUser user) {
        Long userId = requireUser(user);
        Course course = courses.findById(courseId)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));
        wishlist.add(userId, course.getId());
    }

    @Transactional
    public void remove(Long courseId, AuthenticatedUser user) {
        wishlist.remove(requireUser(user), courseId);
    }

    private static Long requireUser(AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để dùng danh sách yêu thích");
        }
        return user.userId();
    }
}
