package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
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
import com.hunre.courseservice.service.impl.CurriculumServiceImpl;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonResourceRepository lessonResourceRepository;

    @InjectMocks
    private CurriculumServiceImpl curriculumService;

    @Test
    @DisplayName("Lấy danh sách giáo trình theo courseId thành công")
    void getCurriculumByCourseId_success() {
        when(courseRepository.existsById(1L)).thenReturn(true);

        Section section = Section.builder()
                .id(10L)
                .title("Chương 1: Giới thiệu")
                .position(1)
                .build();

        when(sectionRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(section));

        List<SectionResponse> result = curriculumService.getCurriculumByCourseId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Chương 1: Giới thiệu");
    }

    @Test
    @DisplayName("Tạo chương học thành công")
    void createSection_success() {
        Course course = Course.builder().id(1L).build();
        CreateSectionRequest request = CreateSectionRequest.builder()
                .courseId(1L)
                .title("Chương 1: Tổng quan")
                .position(1)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(sectionRepository.save(any(Section.class))).thenAnswer(i -> {
            Section s = i.getArgument(0);
            s.setId(10L);
            s.setCreatedAt(LocalDateTime.now());
            return s;
        });

        SectionResponse response = curriculumService.createSection(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Chương 1: Tổng quan");
    }

    @Test
    @DisplayName("Tạo bài học tự động cập nhật tổng số bài và thời lượng của Course")
    void createLesson_success_updatesCourseStats() {
        Course course = Course.builder()
                .id(1L)
                .totalLessons(2)
                .totalDurationSeconds(600)
                .build();

        Section section = Section.builder()
                .id(10L)
                .course(course)
                .title("Chương 1")
                .build();

        CreateLessonRequest request = CreateLessonRequest.builder()
                .sectionId(10L)
                .title("Bài 3: Cài đặt môi trường")
                .type(LessonType.VIDEO)
                .durationSeconds(300)
                .isPreview(true)
                .build();

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> {
            Lesson l = i.getArgument(0);
            l.setId(100L);
            return l;
        });

        LessonResponse response = curriculumService.createLesson(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Bài 3: Cài đặt môi trường");
        assertThat(response.getIsPreview()).isTrue();

        // Kiểm tra Course đã được cập nhật
        assertThat(course.getTotalLessons()).isEqualTo(3);
        assertThat(course.getTotalDurationSeconds()).isEqualTo(900);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("Cập nhật thời lượng bài học tự động điều chỉnh tổng thời lượng của Course")
    void updateLesson_durationChange_updatesCourseStats() {
        Course course = Course.builder()
                .id(1L)
                .totalDurationSeconds(1000)
                .build();

        Lesson lesson = Lesson.builder()
                .id(100L)
                .course(course)
                .title("Bài 1")
                .durationSeconds(300)
                .build();

        UpdateLessonRequest request = UpdateLessonRequest.builder()
                .title("Bài 1 - Cập nhật")
                .durationSeconds(500) // Tăng 200s
                .build();

        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        curriculumService.updateLesson(100L, request);

        assertThat(course.getTotalDurationSeconds()).isEqualTo(1200);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("Xóa bài học tự động giảm số bài học và thời lượng của Course")
    void deleteLesson_success_updatesCourseStats() {
        Course course = Course.builder()
                .id(1L)
                .totalLessons(5)
                .totalDurationSeconds(1500)
                .build();

        Lesson lesson = Lesson.builder()
                .id(100L)
                .course(course)
                .durationSeconds(300)
                .build();

        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

        curriculumService.deleteLesson(100L);

        verify(lessonRepository).delete(lesson);
        assertThat(course.getTotalLessons()).isEqualTo(4);
        assertThat(course.getTotalDurationSeconds()).isEqualTo(1200);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("Thêm tài liệu đính kèm vào bài học thành công")
    void addResource_success() {
        Lesson lesson = Lesson.builder().id(100L).build();
        CreateLessonResourceRequest request = CreateLessonResourceRequest.builder()
                .name("Slide bài giảng")
                .fileUrl("https://storage.elearning.com/slides/intro.pdf")
                .build();

        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
        when(lessonResourceRepository.save(any(LessonResource.class))).thenAnswer(i -> {
            LessonResource r = i.getArgument(0);
            r.setId(50L);
            return r;
        });

        LessonResourceResponse response = curriculumService.addResource(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getName()).isEqualTo("Slide bài giảng");
    }
}
