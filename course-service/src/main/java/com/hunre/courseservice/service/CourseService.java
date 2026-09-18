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

    PageResponse<CourseSummaryResponse> getPublishedCourses(Long categoryId, CourseLevel level, String keyword, Pageable pageable);

    PageResponse<CourseSummaryResponse> getInstructorCourses(Long instructorId, Pageable pageable);

    CourseResponse getCourseById(Long id);

    CourseResponse getCourseBySlug(String slug);

    CourseResponse createCourse(CreateCourseRequest request);

    CourseResponse updateCourse(Long id, UpdateCourseRequest request);

    CourseResponse changeCourseStatus(Long id, ChangeCourseStatusRequest request);

    void deleteCourse(Long id);
}
