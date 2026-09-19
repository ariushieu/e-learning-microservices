package com.hunre.enrollmentservice.client;

import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCourseClient implements CourseClient {

    private final CourseSnapshotRepository courseSnapshotRepository;

    @Override
    public Optional<CourseDto> getCourseById(Long courseId) {
        if (courseId == null || courseId <= 0) {
            return Optional.empty();
        }

        // Tra cứu trực tiếp từ bản sao course_snapshots do Kafka đồng bộ
        return courseSnapshotRepository.findById(courseId)
                .filter(snapshot -> "PUBLISHED".equalsIgnoreCase(snapshot.getStatus()))
                .map(s -> CourseDto.builder()
                        .id(s.getCourseId())
                        .title(s.getTitle())
                        .status(s.getStatus())
                        .totalLessons(s.getTotalLessons() != null ? s.getTotalLessons() : 0)
                        .build());
    }
}
