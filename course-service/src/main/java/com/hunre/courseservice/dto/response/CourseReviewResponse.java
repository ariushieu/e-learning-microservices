package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.CourseReview;
import java.time.Instant;

public record CourseReviewResponse(Long id, int rating, String comment, String authorName,
                                   Instant createdAt, Instant updatedAt, String reply, Instant repliedAt,
                                   String replyAuthorRole) {
    public static CourseReviewResponse from(CourseReview review, Long instructorId) {
        return new CourseReviewResponse(review.getId(), review.getRating(), review.getComment(),
                review.getAuthorName() == null ? "Học viên" : review.getAuthorName(),
                review.getCreatedAt(), review.getUpdatedAt(), review.getReply(), review.getRepliedAt(),
                review.getReply() == null ? null : java.util.Objects.equals(review.getRepliedBy(), instructorId) ? "INSTRUCTOR" : "ADMIN");
    }
}
