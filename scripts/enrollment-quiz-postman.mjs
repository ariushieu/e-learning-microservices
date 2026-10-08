/** Nhóm ENROLL-13 dùng API thật để tạo, làm và chấm quiz; không sửa SQL/snapshot. */
export function addQuizProgressCases({ folder, R, GET, V, save, eq, cv, id }) {
  folder("8. ENROLL-13 — quiz tự hoàn thành bài học", "Quiz → Kafka → tiến độ → chứng chỉ → thông báo. Poll có giới hạn; ENROLL-13.6 giao trùng và lỗi DB được kiểm bằng test Kafka/transaction, không giả lập bằng một lần submit HTTP khác.");
  const setup = { stopOnFailure: true };
  R("ENROLL-13.SETUP Khóa hai bài", "POST", "/api/courses", 201, {
    categoryId: V("categoryId"), title: "Quiz tự hoàn thành " + V("runId"),
    slug: "quiz-progress-" + V("runId"), description: "Fixture Kafka progress", price: 0, level: "BEGINNER", language: "vi",
  }, "tokenA", save("quizProgressCourseId") + save("quizProgressTitle", "d.title"), setup);
  R("ENROLL-13.SETUP Chương", "POST", "/api/courses/" + V("quizProgressCourseId") + "/sections", 201,
    { title: "Chương quiz", position: 0 }, "tokenA", save("quizProgressSectionId"), setup);
  for (const n of [1, 2]) {
    R("ENROLL-13.SETUP Bài " + n, "POST", "/api/sections/" + V("quizProgressSectionId") + "/lessons", 201,
      { title: "Bài quiz " + n, type: "ARTICLE", content: "Học bài rồi làm bài kiểm tra.", durationSeconds: 60, position: n },
      "tokenA", save("quizProgressLesson" + n + "Id"), setup);
  }
  R("ENROLL-13.SETUP Xuất bản đề cương", "PATCH", "/api/courses/" + V("quizProgressCourseId") + "/status", 200,
    { status: "PUBLISHED" }, "tokenA", "", setup);
  for (const [prefix, lessonId] of [["linked", V("quizProgressLesson2Id")], ["unlinked", null]]) {
    R("ENROLL-13.SETUP Tạo quiz " + prefix, "POST", "/api/quizzes", 201,
      { courseId: V("quizProgressCourseId"), lessonId, title: "Quiz " + prefix + " " + V("runId"),
        passScore: 50, maxAttempts: 0, shuffleQuestions: false, shuffleOptions: false },
      "tokenA", save(prefix + "QuizId") + save(prefix + "QuizTitle", "d.title"), setup);
    R("ENROLL-13.SETUP Câu hỏi " + prefix, "POST", "/api/quizzes/" + V(prefix + "QuizId") + "/questions", 201,
      { content: "2 + 2 = ?", type: "SINGLE_CHOICE", score: 1, position: 0,
        options: [{ content: "4", isCorrect: true, position: 0 }, { content: "5", isCorrect: false, position: 1 }] },
      "tokenA", save(prefix + "QuestionId") + save(prefix + "CorrectId", "d.options.find(o => o.isCorrect).id"), setup);
    R("ENROLL-13.SETUP Xuất bản quiz " + prefix, "PATCH", "/api/quizzes/" + V(prefix + "QuizId") + "/status", 200,
      { status: "PUBLISHED" }, "tokenA", "", setup);
  }
  for (const [token, key] of [["studentToken", "quizProgressEnrollmentId"], ["tokenB", "quizCancelledEnrollmentId"]]) {
    R("ENROLL-13.SETUP Ghi danh " + token, "POST", "/api/enrollments", 201,
      { courseId: V("quizProgressCourseId") }, token, save(key),
      { poll: "pm.response.code === 201", maxPolls: 12, pollInterval: 1000, stopOnFailure: true });
  }
  R("ENROLL-13.SETUP S đã xong bài 1", "PUT", "/api/lessons/" + V("quizProgressLesson1Id") + "/progress", 200,
    { courseId: V("quizProgressCourseId"), status: "COMPLETED", watchedSeconds: 60 }, "studentToken");

  const start = (label, prefix, token) => R(label + " Bắt đầu", "POST", "/api/quizzes/" + V(prefix + "QuizId") + "/attempts",
    201, undefined, token, save("quizProgressAttemptId"), setup);
  const submit = (label, prefix, token, passed) => R(label + " Nộp bài", "POST", "/api/attempts/" + V("quizProgressAttemptId") + "/submit",
    200, { answers: passed ? [{ questionId: V(prefix + "QuestionId"), selectedOptionIds: [V(prefix + "CorrectId")] }] : [] },
    token, eq("d.passed", String(passed)) + eq("Number(d.score)", passed ? "100" : "0"));
  const notification = (label, token) => GET(label + " Sự kiện đã tới notification", "/api/notifications?size=100", 200, token,
    "pm.expect(d.content.some(n => n.type === 'QUIZ_GRADED' && n.linkUrl === '/attempts/' + cv('quizProgressAttemptId'))).to.eql(true);",
    { poll: "d && d.content && d.content.some(n => n.type === 'QUIZ_GRADED' && n.linkUrl === '/attempts/' + cv('quizProgressAttemptId'))", maxPolls: 12, pollInterval: 1000 });
  const unchanged = (label) => GET(label + " Tiến độ S không đổi", "/api/progress?courseId=" + V("quizProgressCourseId"), 200, "studentToken",
    eq("d.status", "'ACTIVE'") + eq("Number(d.progressPercent)", "50") +
    "pm.expect(d.lessons.some(l => l.lessonId === id('quizProgressLesson2Id') && l.status === 'COMPLETED')).to.eql(false);" +
    "const key = 'observe:' + pm.info.requestName; const n = Number(cv(key) || 0); if (n < 4) { pm.collectionVariables.set(key, n + 1); setTimeout(() => pm.execution.setNextRequest(pm.info.requestName), 1000); } else { pm.collectionVariables.unset(key); }");

  start("ENROLL-13.2 Trượt", "linked", "studentToken");
  submit("ENROLL-13.2 Trượt", "linked", "studentToken", false);
  notification("ENROLL-13.2", "studentToken");
  unchanged("ENROLL-13.2");

  start("ENROLL-13.3 Không gắn bài", "unlinked", "studentToken");
  submit("ENROLL-13.3 Không gắn bài", "unlinked", "studentToken", true);
  notification("ENROLL-13.3", "studentToken");
  unchanged("ENROLL-13.3");

  start("ENROLL-13.4 Tác giả làm thử", "linked", "tokenA");
  submit("ENROLL-13.4 Tác giả làm thử", "linked", "tokenA", true);
  notification("ENROLL-13.4", "tokenA");
  GET("ENROLL-13.4 Không tự ghi danh tác giả", "/api/progress?courseId=" + V("quizProgressCourseId"), 404, "tokenA");

  start("ENROLL-13.5 B bắt đầu trước khi hủy ghi danh", "linked", "tokenB");
  R("ENROLL-13.5 Hủy ghi danh B", "PATCH", "/api/enrollments/" + V("quizCancelledEnrollmentId") + "/status", 200,
    { status: "CANCELLED" }, "tokenB", eq("d.status", "'CANCELLED'"));
  submit("ENROLL-13.5 Đạt sau khi hủy ghi danh", "linked", "tokenB", true);
  notification("ENROLL-13.5", "tokenB");
  GET("ENROLL-13.5 Không khôi phục ghi danh đã hủy", "/api/enrollments/" + V("quizCancelledEnrollmentId"), 200,
    "tokenB", eq("d.status", "'CANCELLED'") + eq("Number(d.progressPercent)", "0"));
  GET("ENROLL-13.5 Không cấp chứng chỉ cho B", "/api/enrollments/" + V("quizCancelledEnrollmentId") + "/certificate", 404, "tokenB");

  start("ENROLL-13.1 S đạt bài cuối", "linked", "studentToken");
  submit("ENROLL-13.1 S đạt bài cuối", "linked", "studentToken", true);
  GET("ENROLL-13.1 Khóa và bài tự hoàn thành", "/api/progress?courseId=" + V("quizProgressCourseId"), 200, "studentToken",
    eq("d.status", "'COMPLETED'") + eq("Number(d.progressPercent)", "100") + eq("d.completedLessonsCount", "2") +
    "pm.expect(d.lessons.find(l => l.lessonId === id('quizProgressLesson2Id')).status).to.eql('COMPLETED');" + save("quizProgressCertificateCode", "d.certificateCode"),
    { poll: "d && d.status === 'COMPLETED'", maxPolls: 12, pollInterval: 1000 });
  GET("ENROLL-13.1 Chứng chỉ lưu tên học viên", "/api/enrollments/" + V("quizProgressEnrollmentId") + "/certificate", 200,
    "studentToken", eq("d.learnerName", cv("studentName")) + eq("d.certificateCode", cv("quizProgressCertificateCode")));
  GET("ENROLL-13.1 Thông báo hoàn thành và chứng chỉ đúng một lần", "/api/notifications?size=100", 200, "studentToken",
    "const matching = d.content.filter(n => n.linkUrl === '/certificates/' + cv('quizProgressEnrollmentId')); " +
    "for (const type of ['COURSE_COMPLETED', 'CERTIFICATE_ISSUED']) pm.expect(matching.filter(n => n.type === type)).to.have.length(1);",
    { poll: "d && d.content && ['COURSE_COMPLETED','CERTIFICATE_ISSUED'].every(type => d.content.some(n => n.type === type && n.linkUrl === '/certificates/' + cv('quizProgressEnrollmentId')))", maxPolls: 12, pollInterval: 1000 });
}
