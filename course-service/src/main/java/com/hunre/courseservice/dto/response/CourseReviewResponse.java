package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.CourseReview;
import java.time.Instant;

public record CourseReviewResponse(Long id, int rating, String comment, String authorName,
                                   Instant createdAt, Instant updatedAt, String reply, Instant repliedAt) {
    public static CourseReviewResponse from(CourseReview review) {
        return new CourseReviewResponse(review.getId(), review.getRating(), review.getComment(),
                review.getAuthorName() == null ? "Học viên" : review.getAuthorName(),
                review.getCreatedAt(), review.getUpdatedAt(), review.getReply(), review.getRepliedAt());
    }
}
