package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.CourseAnnouncement;
import java.time.Instant;

/** {@code recipientCount} chỉ trả cho người quản lý khóa; học viên nhận null. */
public record CourseAnnouncementResponse(Long id, Long courseId, String title, String content,
                                         String authorName, Integer recipientCount, Instant createdAt) {
    public static CourseAnnouncementResponse from(CourseAnnouncement a, boolean manager) {
        return new CourseAnnouncementResponse(a.getId(), a.getCourseId(), a.getTitle(), a.getContent(),
                a.getAuthorName() == null ? "Giảng viên" : a.getAuthorName(),
                manager ? a.getRecipientCount() : null, a.getCreatedAt());
    }
}
