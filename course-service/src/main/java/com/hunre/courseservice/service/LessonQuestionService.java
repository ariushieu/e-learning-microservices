package com.hunre.courseservice.service;

import com.hunre.courseservice.client.EnrollmentAccessClient;
import com.hunre.courseservice.dto.response.LessonQuestionResponse;
import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.entity.Lesson;
import com.hunre.courseservice.entity.LessonAnswer;
import com.hunre.courseservice.entity.LessonAnswer.AuthorRole;
import com.hunre.courseservice.entity.LessonQuestion;
import com.hunre.courseservice.event.CourseEventPublisher;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.LessonAnswerRepository;
import com.hunre.courseservice.repository.LessonQuestionRepository;
import com.hunre.courseservice.repository.LessonRepository;
import com.hunre.courseservice.util.TextPreview;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.event.LessonQuestionAnsweredEvent;
import com.hunre.sharedcommon.event.LessonQuestionPostedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hỏi đáp trong bài học. Ai đọc được nội dung bài (học viên đang ghi danh, giảng viên của khóa,
 * quản trị viên) thì hỏi và trả lời được; học viên trả lời giúp nhau cũng được.
 *
 * <p>Câu hỏi mới báo cho giảng viên, câu trả lời mới báo cho người hỏi, qua outbox lên Kafka.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonQuestionService {
    private static final EnumSet<AuthorRole> STAFF = EnumSet.of(AuthorRole.INSTRUCTOR, AuthorRole.ADMIN);

    private final LessonQuestionRepository questions;
    private final LessonAnswerRepository answers;
    private final LessonRepository lessons;
    private final CourseRepository courses;
    private final EnrollmentAccessClient enrollmentAccess;
    private final CourseEventPublisher publisher;

    public PageResponse<LessonQuestionResponse> list(Long lessonId, int page, int size, AuthenticatedUser user) {
        Lesson lesson = requireAccess(lessonId, user);
        boolean manager = isManager(lesson.getCourse(), user);
        Page<LessonQuestion> result = questions.findByLessonIdOrderByLastActivityAtDescIdDesc(lessonId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponse.of(withAnswers(result.getContent(), user.userId(), q -> manager, q -> null, q -> null),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public LessonQuestionResponse ask(Long lessonId, String content, AuthenticatedUser user) {
        Lesson lesson = requireAccess(lessonId, user);
        Course course = lesson.getCourse();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        LessonQuestion question = new LessonQuestion();
        question.setLessonId(lessonId);
        question.setCourseId(course.getId());
        question.setUserId(user.userId());
        question.setAuthorName(displayName(user));
        question.setContent(content.strip());
        question.setLastActivityAt(now);
        questions.saveAndFlush(question);

        if (!user.userId().equals(course.getInstructorId())) {
            publisher.publishLessonEvent(LessonQuestionPostedEvent.of(question.getId(), lessonId, course.getId(),
                    course.getTitle(), lesson.getTitle(), user.userId(), question.getAuthorName(),
                    course.getInstructorId(), TextPreview.of(question.getContent())), course.getId());
        }
        return LessonQuestionResponse.from(question, List.of(), user.userId(), isManager(course, user), null, null);
    }

    @Transactional
    public LessonQuestionResponse answer(Long lessonId, Long questionId, String content, AuthenticatedUser user) {
        Lesson lesson = requireAccess(lessonId, user);
        Course course = lesson.getCourse();
        LessonQuestion question = questions.findByIdAndLessonId(questionId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("câu hỏi", "id", questionId));

        LessonAnswer answer = new LessonAnswer();
        answer.setQuestionId(questionId);
        answer.setUserId(user.userId());
        answer.setAuthorName(displayName(user));
        answer.setAuthorRole(user.userId().equals(course.getInstructorId()) ? AuthorRole.INSTRUCTOR
                : user.hasRole(Roles.ADMIN) ? AuthorRole.ADMIN : AuthorRole.STUDENT);
        answer.setContent(content.strip());
        answers.saveAndFlush(answer);

        refreshCounters(question);
        question.setLastActivityAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        questions.save(question);

        if (!user.userId().equals(question.getUserId())) {
            publisher.publishLessonEvent(LessonQuestionAnsweredEvent.of(questionId, answer.getId(), lessonId,
                    course.getId(), lesson.getTitle(), question.getUserId(), user.userId(), answer.getAuthorName(),
                    answer.getAuthorRole().name(), TextPreview.of(answer.getContent())), course.getId());
        }
        boolean manager = isManager(course, user);
        return withAnswers(List.of(question), user.userId(), q -> manager, q -> null, q -> null).get(0);
    }

    @Transactional
    public void deleteQuestion(Long lessonId, Long questionId, AuthenticatedUser user) {
        Lesson lesson = requireLesson(lessonId, user);
        LessonQuestion question = questions.findByIdAndLessonId(questionId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("câu hỏi", "id", questionId));
        requireAuthorOrManager(question.getUserId(), lesson.getCourse(), user);
        questions.delete(question);
    }

    @Transactional
    public void deleteAnswer(Long lessonId, Long questionId, Long answerId, AuthenticatedUser user) {
        Lesson lesson = requireLesson(lessonId, user);
        LessonQuestion question = questions.findByIdAndLessonId(questionId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("câu hỏi", "id", questionId));
        LessonAnswer answer = answers.findByIdAndQuestionId(answerId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("câu trả lời", "id", answerId));
        requireAuthorOrManager(answer.getUserId(), lesson.getCourse(), user);
        answers.delete(answer);
        answers.flush();
        refreshCounters(question);
        questions.save(question);
    }

    /** Hộp câu hỏi của giảng viên trên mọi khóa mình dạy; quản trị viên thấy tất cả. */
    public PageResponse<LessonQuestionResponse> inbox(Boolean answered, Long courseId, int page, int size,
                                                      AuthenticatedUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên hoặc quản trị viên xem được hộp câu hỏi");
        }
        Long instructorId = user.hasRole(Roles.ADMIN) ? null : user.userId();
        if (courseId != null && instructorId != null) {
            Course course = courses.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));
            if (!instructorId.equals(course.getInstructorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không phải giảng viên của khóa học này");
            }
        }
        Page<LessonQuestion> result = questions.findInbox(instructorId, answered, courseId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        Map<Long, Lesson> lessonById = lessons.findAllById(result.getContent().stream().map(LessonQuestion::getLessonId)
                .distinct().toList()).stream().collect(Collectors.toMap(Lesson::getId, Function.identity()));
        return PageResponse.of(withAnswers(result.getContent(), user.userId(), q -> true,
                        q -> lessonById.containsKey(q.getLessonId()) ? lessonById.get(q.getLessonId()).getTitle() : null,
                        q -> lessonById.containsKey(q.getLessonId()) ? lessonById.get(q.getLessonId()).getCourse().getTitle() : null),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public long countUnanswered(AuthenticatedUser user) {
        if (user == null || !user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) return 0;
        return questions.countUnanswered(user.hasRole(Roles.ADMIN) ? null : user.userId());
    }

    private List<LessonQuestionResponse> withAnswers(List<LessonQuestion> page, Long viewerId,
                                                     Function<LessonQuestion, Boolean> manager,
                                                     Function<LessonQuestion, String> lessonTitle,
                                                     Function<LessonQuestion, String> courseTitle) {
        if (page.isEmpty()) return List.of();
        Map<Long, List<LessonAnswer>> byQuestion = answers.findByQuestionIdInOrderByCreatedAtAscIdAsc(
                page.stream().map(LessonQuestion::getId).toList()).stream()
                .collect(Collectors.groupingBy(LessonAnswer::getQuestionId));
        return page.stream().map(q -> {
            boolean m = manager.apply(q);
            var list = byQuestion.getOrDefault(q.getId(), List.of()).stream()
                    .map(a -> LessonQuestionResponse.Answer.from(a, viewerId, m)).toList();
            return LessonQuestionResponse.from(q, list, viewerId, m, lessonTitle.apply(q), courseTitle.apply(q));
        }).toList();
    }

    private void refreshCounters(LessonQuestion question) {
        question.setAnswerCount((int) answers.countByQuestionId(question.getId()));
        question.setInstructorAnswered(answers.existsByQuestionIdAndAuthorRoleIn(question.getId(), STAFF));
    }

    /** Đọc và ghi hỏi đáp cần cùng quyền với đọc nội dung bài học có bảo vệ. */
    private Lesson requireAccess(Long lessonId, AuthenticatedUser user) {
        Lesson lesson = requireLesson(lessonId, user);
        Course course = lesson.getCourse();
        if (isManager(course, user)) return lesson;
        boolean open = course.getStatus() == CourseStatus.PUBLISHED || course.getStatus() == CourseStatus.ARCHIVED;
        if (!open) throw new ResourceNotFoundException("bài học", "id", lessonId);
        if (!enrollmentAccess.hasEnrollment(course.getId(), user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ học viên đã ghi danh mới tham gia hỏi đáp của bài học");
        }
        return lesson;
    }

    private Lesson requireLesson(Long lessonId, AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để tham gia hỏi đáp");
        }
        return lessons.findById(lessonId).orElseThrow(() -> new ResourceNotFoundException("bài học", "id", lessonId));
    }

    private static void requireAuthorOrManager(Long authorId, Course course, AuthenticatedUser user) {
        if (!authorId.equals(user.userId()) && !isManager(course, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn chỉ xóa được nội dung của chính mình");
        }
    }

    private static boolean isManager(Course course, AuthenticatedUser user) {
        return user.hasRole(Roles.ADMIN) || user.userId().equals(course.getInstructorId());
    }

    private static String displayName(AuthenticatedUser user) {
        return user.fullName() == null || user.fullName().isBlank() ? null : user.fullName().strip();
    }
}
