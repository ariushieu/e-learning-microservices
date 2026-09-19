package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.CourseSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSnapshotRepository extends JpaRepository<CourseSnapshot, Long> {
}
