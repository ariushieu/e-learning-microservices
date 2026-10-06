package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    @Query("SELECT s.course.id FROM Section s WHERE s.id = :id")
    Optional<Long> findCourseIdById(@Param("id") Long id);

    List<Section> findByCourseIdOrderByPositionAsc(Long courseId);

    boolean existsByCourseId(Long courseId);
}
