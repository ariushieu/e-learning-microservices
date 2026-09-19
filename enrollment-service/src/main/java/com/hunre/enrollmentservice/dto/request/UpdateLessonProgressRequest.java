package com.hunre.enrollmentservice.dto.request;

import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu cập nhật tiến độ học tập cho một bài học.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLessonProgressRequest {

    @NotNull(message = "ID khóa học không được để trống")
    @Positive(message = "ID khóa học phải là số dương")
    private Long courseId;

    @NotNull(message = "ID bài học không được để trống")
    @Positive(message = "ID bài học phải là số dương")
    private Long lessonId;

    @NotNull(message = "Trạng thái bài học không được để trống")
    private LessonProgressStatus status;

    @Min(value = 0, message = "Thời lượng xem phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private Integer watchedSeconds = 0;

    /**
     * Tùy chọn truyền userId khi test thủ công không qua JWT.
     */
    private Long userId;
}
