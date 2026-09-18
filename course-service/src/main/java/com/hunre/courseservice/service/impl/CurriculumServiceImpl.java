package com.hunre.courseservice.service.impl;

import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
import com.hunre.courseservice.dto.request.UpdateSectionRequest;
import com.hunre.courseservice.dto.response.LessonResourceResponse;
import com.hunre.courseservice.dto.response.LessonResponse;
import com.hunre.courseservice.dto.response.SectionResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.Lesson;
import com.hunre.courseservice.entity.LessonResource;
import com.hunre.courseservice.entity.LessonType;
import com.hunre.courseservice.entity.Section;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.LessonRepository;
import com.hunre.courseservice.repository.LessonResourceRepository;
import com.hunre.courseservice.repository.SectionRepository;
import com.hunre.courseservice.service.CurriculumService;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumServiceImpl implements CurriculumService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;

    @Override
    public List<SectionResponse> getCurriculumByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("khóa học", "id", courseId);
        }

        List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAsc(courseId);
        return sections.stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", request.getCourseId()));

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle().trim())
                .position(request.getPosition() != null ? request.getPosition() : 0)
                .build();

        Section saved = sectionRepository.save(section);
        return SectionResponse.from(saved);
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long id, UpdateSectionRequest request) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", id));

        section.setTitle(request.getTitle().trim());
        section.setPosition(request.getPosition() != null ? request.getPosition() : 0);

        Section updated = sectionRepository.save(section);
        return SectionResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteSection(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", id));

        Course course = section.getCourse();
        sectionRepository.delete(section);

        // Tính toán lại tổng số bài học và tổng thời lượng của khóa học
        updateCourseStats(course.getId());
    }

    @Override
    public LessonResponse getLessonById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));
        return LessonResponse.from(lesson);
    }

    @Override
    @Transactional
    public LessonResponse createLesson(CreateLessonRequest request) {
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("chương học", "id", request.getSectionId()));

        Course course = section.getCourse();
        int duration = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;

        Lesson lesson = Lesson.builder()
                .section(section)
                .course(course)
                .title(request.getTitle().trim())
                .type(request.getType() != null ? request.getType() : LessonType.VIDEO)
                .durationSeconds(duration)
                .position(request.getPosition() != null ? request.getPosition() : 0)
                .isPreview(Boolean.TRUE.equals(request.getIsPreview()))
                .build();

        Lesson saved = lessonRepository.save(lesson);

        // Cập nhật số liệu dẫn xuất của khóa học
        course.setTotalLessons(course.getTotalLessons() + 1);
        course.setTotalDurationSeconds(course.getTotalDurationSeconds() + duration);
        courseRepository.save(course);

        return LessonResponse.from(saved);
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(Long id, UpdateLessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));

        int oldDuration = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;
        int newDuration = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;
        int durationDiff = newDuration - oldDuration;

        lesson.setTitle(request.getTitle().trim());
        lesson.setType(request.getType() != null ? request.getType() : LessonType.VIDEO);
        lesson.setDurationSeconds(newDuration);
        lesson.setPosition(request.getPosition() != null ? request.getPosition() : 0);
        lesson.setIsPreview(Boolean.TRUE.equals(request.getIsPreview()));

        Lesson updated = lessonRepository.save(lesson);

        if (durationDiff != 0) {
            Course course = updated.getCourse();
            course.setTotalDurationSeconds(Math.max(0, course.getTotalDurationSeconds() + durationDiff));
            courseRepository.save(course);
        }

        return LessonResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", id));

        Course course = lesson.getCourse();
        int duration = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;

        lessonRepository.delete(lesson);

        // Cập nhật lại số liệu dẫn xuất
        course.setTotalLessons(Math.max(0, course.getTotalLessons() - 1));
        course.setTotalDurationSeconds(Math.max(0, course.getTotalDurationSeconds() - duration));
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public LessonResourceResponse addResource(Long lessonId, CreateLessonResourceRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("bài học", "id", lessonId));

        LessonResource resource = LessonResource.builder()
                .lesson(lesson)
                .name(request.getName().trim())
                .fileUrl(request.getFileUrl().trim())
                .build();

        LessonResource saved = lessonResourceRepository.save(resource);
        return LessonResourceResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId) {
        LessonResource resource = lessonResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("tài liệu bài học", "id", resourceId));

        lessonResourceRepository.delete(resource);
    }

    private void updateCourseStats(Long courseId) {
        courseRepository.findById(courseId).ifPresent(course -> {
            course.setTotalLessons(lessonRepository.countByCourseId(courseId));
            course.setTotalDurationSeconds(lessonRepository.sumDurationSecondsByCourseId(courseId));
            courseRepository.save(course);
        });
    }
}
