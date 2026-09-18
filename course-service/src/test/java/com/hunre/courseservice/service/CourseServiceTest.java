package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.request.UpdateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.entity.Category;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CategoryRepository;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.service.impl.CourseServiceImpl;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    @DisplayName("Tạo khóa học thành công")
    void createCourse_success() {
        Category category = Category.builder().id(1L).name("CNTT").build();
        CreateCourseRequest request = CreateCourseRequest.builder()
                .categoryId(1L)
                .instructorId(100L)
                .instructorName("Thầy Tuấn")
                .title("Lập trình Microservices với Spring Boot")
                .summary("Khóa học Microservices thực chiến")
                .price(BigDecimal.valueOf(500000))
                .level(CourseLevel.INTERMEDIATE)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(courseRepository.existsBySlug("lap-trinh-microservices-voi-spring-boot")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(10L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        CourseResponse response = courseService.createCourse(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Lập trình Microservices với Spring Boot");
        assertThat(response.getSlug()).isEqualTo("lap-trinh-microservices-voi-spring-boot");
        assertThat(response.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.getCategoryName()).isEqualTo("CNTT");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Tạo khóa học thất bại khi slug bị trùng")
    void createCourse_duplicateSlug_throwsDuplicateResourceException() {
        Category category = Category.builder().id(1L).name("CNTT").build();
        CreateCourseRequest request = CreateCourseRequest.builder()
                .categoryId(1L)
                .instructorId(100L)
                .title("Java Cơ Bản")
                .slug("java-co-ban")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(courseRepository.existsBySlug("java-co-ban")).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("java-co-ban");

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo khóa học thất bại khi danh mục không tồn tại")
    void createCourse_categoryNotFound_throwsResourceNotFoundException() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .categoryId(999L)
                .instructorId(100L)
                .title("Java")
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Cập nhật khóa học thất bại khi khóa học đã lưu trữ (ARCHIVED)")
    void updateCourse_archived_throwsBusinessException() {
        Course course = Course.builder()
                .id(1L)
                .status(CourseStatus.ARCHIVED)
                .build();

        UpdateCourseRequest request = UpdateCourseRequest.builder()
                .categoryId(1L)
                .title("New Title")
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.updateCourse(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ARCHIVED");
    }

    @Test
    @DisplayName("Chuyển trạng thái sang PUBLISHED sẽ tự động gán publishedAt")
    void changeCourseStatus_published_setsPublishedAt() {
        Course course = Course.builder()
                .id(1L)
                .status(CourseStatus.DRAFT)
                .publishedAt(null)
                .build();

        ChangeCourseStatusRequest request = ChangeCourseStatusRequest.builder()
                .status(CourseStatus.PUBLISHED)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse response = courseService.changeCourseStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(course.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Xóa khóa học thất bại khi không ở trạng thái DRAFT")
    void deleteCourse_notDraft_throwsBusinessException() {
        Course course = Course.builder()
                .id(1L)
                .status(CourseStatus.PUBLISHED)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.deleteCourse(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");

        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    @DisplayName("Xóa khóa học thất bại khi đã có học viên đăng ký")
    void deleteCourse_hasStudents_throwsBusinessException() {
        Course course = Course.builder()
                .id(1L)
                .status(CourseStatus.DRAFT)
                .studentCount(5)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.deleteCourse(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã có học viên đăng ký");

        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    @DisplayName("Xóa khóa học thành công khi là DRAFT và chưa có học viên")
    void deleteCourse_success() {
        Course course = Course.builder()
                .id(1L)
                .status(CourseStatus.DRAFT)
                .studentCount(0)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(1L);

        verify(courseRepository).delete(course);
    }
}
