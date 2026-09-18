package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseIdOrderByPositionAsc(Long courseId);

    boolean existsByCourseId(Long courseId);
}
