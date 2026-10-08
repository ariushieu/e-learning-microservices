package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.QuizResultsResponse;
import com.hunre.quizservice.dto.QuizResultsResponse.*;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class QuizResultsService {
    private final QuizRepository quizzes;
    private final JdbcTemplate jdbc;

    // Aggregate in the database; never load all learner attempts to page in Java.
    private static final String ELIGIBLE = "quiz_id = ? AND is_preview = false AND user_id <> ?";
    private static final String SUBMITTED = ELIGIBLE + " AND status = 'SUBMITTED'";
    private static final String GROUPED = "SELECT user_id, COUNT(*) AS attempt_count, MAX(score) AS best_score, "
            + "MAX(CASE WHEN passed = true THEN 1 ELSE 0 END) AS has_passed, "
            + "MAX(submitted_at) AS last_submitted_at, MAX(id) AS latest_id "
            + "FROM quiz_attempts WHERE " + SUBMITTED + " GROUP BY user_id";
    private static final String LEARNERS = "SELECT g.*, a.learner_name FROM (" + GROUPED + ") g "
            + "JOIN quiz_attempts a ON a.id = g.latest_id ORDER BY g.last_submitted_at ";
    private static final RowMapper<Learner> LEARNER_MAPPER = (rs, row) -> {
        String name = rs.getString("learner_name");
        long id = rs.getLong("user_id");
        return new Learner(id, name == null || name.isBlank() ? "Học viên #" + id : name,
                rs.getLong("attempt_count"), rs.getBigDecimal("best_score"), rs.getInt("has_passed") > 0,
                rs.getTimestamp("last_submitted_at", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant());
    };
    private static final DateTimeFormatter VIETNAM_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Transactional(readOnly = true)
    public QuizResultsResponse getResults(Long quizId, AuthenticatedUser user, Pageable pageable) {
        var quiz = requireAccess(quizId, user);
        if (pageable.getSort().stream().anyMatch(o -> !o.getProperty().equals("lastSubmittedAt"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ hỗ trợ sắp xếp theo lastSubmittedAt");
        }
        Long owner = quiz.getCreatedBy();
        var totals = jdbc.queryForMap("SELECT COUNT(*) AS learners, COALESCE(SUM(attempt_count),0) AS attempts, "
                + "COALESCE(AVG(best_score),0) AS average_score, COALESCE(SUM(has_passed),0) AS passed "
                + "FROM (" + GROUPED + ") g", quizId, owner);
        long learnerCount = ((Number) totals.get("learners")).longValue();
        long submittedCount = ((Number) totals.get("attempts")).longValue();
        long passedCount = ((Number) totals.get("passed")).longValue();
        var average = new BigDecimal(totals.get("average_score").toString()).setScale(2, RoundingMode.HALF_UP);
        long expired = jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE " + ELIGIBLE
                + " AND status = 'EXPIRED'", Long.class, quizId, owner);
        long unclassified = jdbc.queryForObject("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id = ? "
                + "AND user_id <> ? AND is_preview IS NULL AND status IN ('SUBMITTED','EXPIRED')",
                Long.class, quizId, owner);
        var direction = pageable.getSort().getOrderFor("lastSubmittedAt");
        String order = direction != null && direction.isAscending() ? "ASC" : "DESC";
        var learners = jdbc.query(LEARNERS + order + ", g.user_id ASC LIMIT ? OFFSET ?",
                LEARNER_MAPPER, quizId, owner, pageable.getPageSize(), pageable.getOffset());
        // Only answers actually graded for a question contribute (questions can be added later).
        var questions = jdbc.query("""
                SELECT q.id, q.content, COUNT(a.id) AS graded,
                       COALESCE(SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END),0) AS correct
                FROM questions q LEFT JOIN (
                    SELECT aa.* FROM attempt_answers aa JOIN quiz_attempts qa ON qa.id = aa.attempt_id
                    WHERE qa.quiz_id = ? AND qa.is_preview = false AND qa.user_id <> ? AND qa.status = 'SUBMITTED'
                ) a ON a.question_id = q.id
                WHERE q.quiz_id = ? AND q.deleted = false GROUP BY q.id, q.content, q.position ORDER BY q.position, q.id
                """, (rs, row) -> new QuestionRate(rs.getLong("id"), rs.getString("content"),
                rs.getLong("graded"), percent(rs.getLong("correct"), rs.getLong("graded"))), quizId, owner, quizId);
        return new QuizResultsResponse(quizId, quiz.getTitle(),
                new Summary(learnerCount, submittedCount, expired, average, percent(passedCount, learnerCount),
                        unclassified, questions),
                PageResponse.of(learners, pageable.getPageNumber(), pageable.getPageSize(), learnerCount));
    }

    @Transactional(readOnly = true)
    public byte[] exportResults(Long quizId, AuthenticatedUser user) {
        var quiz = requireAccess(quizId, user);
        var learners = jdbc.query(LEARNERS + "DESC, g.user_id ASC", LEARNER_MAPPER, quizId, quiz.getCreatedBy());
        var csv = new StringBuilder("\uFEFFHọc viên,Lượt nộp,Điểm cao nhất,Kết quả,Nộp gần nhất (giờ Việt Nam)\r\n");
        for (var learner : learners) {
            csv.append(csvCell(learner.learnerName())).append(',')
                    .append(learner.submittedAttempts()).append(',')
                    .append(learner.bestScore().setScale(2, RoundingMode.HALF_UP).toPlainString()).append(',')
                    .append(learner.passed() ? "Đạt" : "Chưa đạt").append(',')
                    .append(VIETNAM_TIME.format(learner.lastSubmittedAt())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Quiz requireAccess(Long quizId, AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) forbidden();
        var quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("bài kiểm tra", "id", quizId));
        if (!user.hasRole(Roles.ADMIN) && !user.userId().equals(quiz.getCreatedBy())) forbidden();
        return quiz;
    }

    private static String csvCell(String value) {
        // Neutralize formulas before CSV quoting: quotes alone do not stop Excel evaluating them.
        if (!value.isEmpty() && "=+-@\t\r\n".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        if (value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static BigDecimal percent(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static void forbidden() {
        throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ tác giả hoặc quản trị viên được xem kết quả bài kiểm tra");
    }
}
