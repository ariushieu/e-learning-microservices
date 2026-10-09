package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Long> {
    Page<CourseAnnouncement> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId, Pageable pageable);

    Optional<CourseAnnouncement> findByIdAndCourseId(Long id, Long courseId);
}
