package com.hunre.courseservice.dto.response;

import com.hunre.sharedcommon.dto.PageResponse;
import java.util.List;

public record InstructorReviewsResponse(PageResponse<Item> reviews, long unrepliedCount, List<CourseOption> courses) {
    public record Item(Long courseId, String courseTitle, CourseReviewResponse review) {}
    public record CourseOption(Long id, String title) {}
}
