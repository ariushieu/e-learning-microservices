package com.hunre.enrollmentservice.service;

import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;

public interface ProgressService {

    /**
     * Cập nhật tiến độ học tập cho một bài học và tự động tính toán lại % hoàn thành khóa học.
     *
     * @param currentUserId ID học viên hiện tại (từ JWT hoặc null)
     * @param request       Dữ liệu cập nhật tiến độ bài học
     * @return Thông tin tiến độ bài học đã cập nhật
     */
    LessonProgressResponse updateLessonProgress(Long currentUserId, UpdateLessonProgressRequest request);

    /**
     * Lấy tiến độ chi tiết của một khóa học đối với học viên.
     *
     * @param currentUserId ID học viên hiện tại (từ JWT hoặc null)
     * @param courseId      ID khóa học cần tra cứu
     * @return Thông tin % hoàn thành và danh sách các bài học
     */
    CourseProgressResponse getCourseProgress(Long currentUserId, Long courseId);
}
