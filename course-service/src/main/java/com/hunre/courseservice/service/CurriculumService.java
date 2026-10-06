package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
import com.hunre.courseservice.dto.request.UpdateSectionRequest;
import com.hunre.courseservice.dto.response.LessonResourceResponse;
import com.hunre.courseservice.dto.response.LessonResponse;
import com.hunre.courseservice.dto.response.SectionResponse;

import java.util.List;

public interface CurriculumService {

    List<SectionResponse> getCurriculumByCourseId(Long courseId);

    SectionResponse createSection(Long courseId, CreateSectionRequest request, Long currentUserId, boolean isAdmin);

    SectionResponse updateSection(Long id, UpdateSectionRequest request, Long currentUserId, boolean isAdmin);

    void deleteSection(Long id, Long currentUserId, boolean isAdmin);

    LessonResponse getLessonById(Long id);

    LessonResponse createLesson(Long sectionId, CreateLessonRequest request, Long currentUserId, boolean isAdmin);

    LessonResponse updateLesson(Long id, UpdateLessonRequest request, Long currentUserId, boolean isAdmin);

    void deleteLesson(Long id, Long currentUserId, boolean isAdmin);

    LessonResourceResponse addResource(Long lessonId, CreateLessonResourceRequest request, Long currentUserId, boolean isAdmin);

    void deleteResource(Long lessonId, Long resourceId, Long currentUserId, boolean isAdmin);
}
