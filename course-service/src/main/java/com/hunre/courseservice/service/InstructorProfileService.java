package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.response.InstructorProfileResponse;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorProfileService {
    private final CourseRepository courses;

    public InstructorProfileResponse getProfile(Long instructorId) {
        var statistics = courses.aggregateInstructor(instructorId, CourseStatus.PUBLISHED);
        if (statistics.getPublishedCourses() == 0) {
            throw new ResourceNotFoundException("giảng viên", "id", instructorId);
        }
        // Tên là snapshot ở khóa công khai mới nhất, không gọi auth hay lấy tên từ khóa nháp.
        var course = courses.findFirstByInstructorIdAndStatusOrderByIdDesc(instructorId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("giảng viên", "id", instructorId));
        String name = course.getInstructorName();
        if (name == null || name.isBlank()) name = "Giảng viên HUNRE";
        long count = statistics.getRatingCount();
        BigDecimal average = count == 0 ? BigDecimal.ZERO.setScale(2)
                : statistics.getWeightedRating().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return new InstructorProfileResponse(name.strip(), statistics.getPublishedCourses(),
                statistics.getTotalStudents(), average, count);
    }
}
