package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {

    List<LessonResource> findByLessonId(Long lessonId);
}
