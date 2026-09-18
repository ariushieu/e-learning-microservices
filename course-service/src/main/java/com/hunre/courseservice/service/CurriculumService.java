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

    SectionResponse createSection(CreateSectionRequest request);

    SectionResponse updateSection(Long id, UpdateSectionRequest request);

    void deleteSection(Long id);

    LessonResponse getLessonById(Long id);

    LessonResponse createLesson(CreateLessonRequest request);

    LessonResponse updateLesson(Long id, UpdateLessonRequest request);

    void deleteLesson(Long id);

    LessonResourceResponse addResource(Long lessonId, CreateLessonResourceRequest request);

    void deleteResource(Long resourceId);
}
