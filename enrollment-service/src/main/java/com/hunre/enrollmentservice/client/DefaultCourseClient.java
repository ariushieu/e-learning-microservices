package com.hunre.enrollmentservice.client;

import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCourseClient implements CourseClient {

    private final CourseSnapshotRepository courseSnapshotRepository;

    // Danh mục khóa học chuẩn với số lượng bài học thực tế
    private static final Map<Long, PredefinedCourse> PREDEFINED_COURSES = Map.of(
            1L, new PredefinedCourse("Kiến trúc Microservices với Spring Boot & Spring Cloud", 5),
            2L, new PredefinedCourse("Thiết kế Cơ sở dữ liệu & Apache Kafka (KRaft Mode)", 4),
            3L, new PredefinedCourse("Docker, Docker Compose & Triển khai Microservices", 3)
    );

    private record PredefinedCourse(String title, int totalLessons) {}

    @Override
    public Optional<CourseDto> getCourseById(Long courseId) {
        if (courseId == null || courseId <= 0) {
            return Optional.empty();
        }

        // 1. Kiểm tra danh mục khóa học chuẩn được cấu hình
        PredefinedCourse predefined = PREDEFINED_COURSES.get(courseId);
        if (predefined != null) {
            return Optional.of(CourseDto.builder()
                    .id(courseId)
                    .title(predefined.title())
                    .status("PUBLISHED")
                    .totalLessons(predefined.totalLessons())
                    .build());
        }

        // 2. Kiểm tra snapshot đã được đồng bộ từ Kafka/DB
        Optional<CourseSnapshot> snapshotOpt = courseSnapshotRepository.findById(courseId);
        if (snapshotOpt.isPresent()) {
            CourseSnapshot s = snapshotOpt.get();
            return Optional.of(CourseDto.builder()
                    .id(s.getCourseId())
                    .title(s.getTitle())
                    .status("PUBLISHED")
                    .totalLessons(s.getTotalLessons() != null && s.getTotalLessons() > 0 ? s.getTotalLessons() : 5)
                    .build());
        }

        // 3. Trả về dữ liệu stub/mock cho môi trường phát triển & kiểm thử
        log.debug("Sử dụng stub dữ liệu cho courseId={}", courseId);
        return Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Khóa học Lập trình Microservices #" + courseId)
                .status("PUBLISHED")
                .totalLessons(5)
                .build());
    }
}
