package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.request.UpdateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.sharedcommon.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    PageResponse<CourseSummaryResponse> getCourses(Long instructorId, Long categoryId, CourseLevel level, String keyword, Pageable pageable);

    CourseResponse getCourseById(Long id);

    CourseResponse getCourseBySlug(String slug);

    CourseResponse createCourse(CreateCourseRequest request, Long instructorId, String instructorName);

    default CourseResponse createCourse(CreateCourseRequest request, Long instructorId) {
        return createCourse(request, instructorId, null);
    }

    CourseResponse updateCourse(Long id, UpdateCourseRequest request, Long currentUserId, boolean isAdmin);

    CourseResponse changeCourseStatus(Long id, ChangeCourseStatusRequest request, Long currentUserId, boolean isAdmin);

    void deleteCourse(Long id, Long currentUserId, boolean isAdmin);

}
