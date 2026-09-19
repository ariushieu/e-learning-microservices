package com.hunre.enrollmentservice.client;

import java.util.Optional;

public interface CourseClient {

    /**
     * Lấy thông tin khóa học từ Course Service hoặc snapshot/stub.
     *
     * @param courseId ID khóa học cần tra cứu
     * @return Thông tin khóa học nếu tồn tại
     */
    Optional<CourseDto> getCourseById(Long courseId);
}
