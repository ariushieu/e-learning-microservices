package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.hunre.courseservice.entity.Course;
import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    Page<CourseReview> findByCourseId(Long courseId, Pageable pageable);
    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);
    Optional<CourseReview> findByIdAndCourseId(Long id, Long courseId);

    @Query(value = """
            select new com.hunre.courseservice.repository.ReviewCourseRow(r, c)
            from CourseReview r join Course c on c.id = r.courseId
            where (:instructorId is null or c.instructorId = :instructorId)
              and (:courseId is null or c.id = :courseId)
              and (:replied is null or (:replied = true and r.reply is not null)
                or (:replied = false and r.reply is null))
            order by r.createdAt desc, r.id desc
            """, countQuery = """
            select count(r) from CourseReview r join Course c on c.id = r.courseId
            where (:instructorId is null or c.instructorId = :instructorId)
              and (:courseId is null or c.id = :courseId)
              and (:replied is null or (:replied = true and r.reply is not null)
                or (:replied = false and r.reply is null))
            """)
    Page<ReviewCourseRow> findInbox(@Param("instructorId") Long instructorId, @Param("courseId") Long courseId,
                                  @Param("replied") Boolean replied, Pageable pageable);

    @Query("""
            select count(r) from CourseReview r join Course c on c.id = r.courseId
            where r.reply is null and (:instructorId is null or c.instructorId = :instructorId)
              and (:courseId is null or c.id = :courseId)
            """)
    long countUnreplied(@Param("instructorId") Long instructorId, @Param("courseId") Long courseId);

    @Query("""
            select distinct c from Course c join CourseReview r on r.courseId = c.id
            where (:instructorId is null or c.instructorId = :instructorId) order by c.title, c.id
            """)
    List<Course> findReviewedCourses(@Param("instructorId") Long instructorId);
}
