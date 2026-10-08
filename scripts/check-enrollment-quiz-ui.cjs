// Chạy sau collection enrollment trên stack Docker dùng riêng cho nghiệm thu.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require(process.env.ENROLLMENT_PLAYWRIGHT_MODULE || "playwright");
const web = process.env.ENROLLMENT_WEB_URL || "http://localhost:3000";
const gateway = process.env.ENROLLMENT_GATEWAY_URL || "http://localhost:8080";
const output = process.env.ENROLLMENT_UI_OUTPUT || path.join(require("node:os").tmpdir(), "enrollment-quiz-ui");
const checks = [], errors = [];
let browser;

async function api(method, url, token, body) {
  const response = await fetch(gateway + url, {
    method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000),
  });
  assert.ok(response.ok, `${method} ${url}: HTTP ${response.status}`);
  return (await response.json()).data;
}

async function run() {
  const instructor = await api("POST", "/api/auth/login", null, { email: "qa.instructor.a@example.com", password: "Test@123456" });
  const courses = await api("GET", `/api/courses?instructorId=${instructor.user.id}&keyword=${encodeURIComponent("Quiz tự hoàn thành")}&size=100`, instructor.accessToken);
  assert.equal(courses.totalElements, 1, "Expected the collection's isolated ENROLL-13 fixture");
  const course = courses.content[0];
  const sections = await api("GET", `/api/courses/${course.id}/curriculum`, instructor.accessToken);
  const lessons = sections.flatMap(section => section.lessons);
  const first = lessons.find(lesson => lesson.title === "Bài quiz 1");
  const last = lessons.find(lesson => lesson.title === "Bài quiz 2");
  assert.ok(first && last);
  const quizzes = await api("GET", `/api/quizzes?courseId=${course.id}`, instructor.accessToken);
  const quiz = quizzes.find(quiz => quiz.lessonId === last.id);
  assert.ok(quiz);
  const questions = await api("GET", `/api/quizzes/${quiz.id}/questions`, instructor.accessToken);

  const email = `qa.quiz-ui.${Date.now()}@example.com`;
  await api("POST", "/api/auth/register", null, { email, password: "Test@123456", fullName: "Học viên nghiệm thu quiz" });
  const learner = await api("POST", "/api/auth/login", null, { email, password: "Test@123456" });
  const token = learner.accessToken;
  const enrollment = await api("POST", "/api/enrollments", token, { courseId: course.id });
  await api("PUT", `/api/lessons/${first.id}/progress`, token, { courseId: course.id, status: "COMPLETED", watchedSeconds: 60 });

  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  await context.addCookies([{ name: "el_access", value: token, url: web }]);
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(web + "/design");
  await page.getByRole("main").waitFor();
  const learnUrl = `${web}/learn/${course.id}?lesson=${last.id}`;
  await page.goto(learnUrl);
  await page.getByText("Đạt bài kiểm tra thì bài này tự hoàn thành.", { exact: true }).waitFor();
  await page.getByRole("button", { name: "Đánh dấu hoàn thành", exact: true }).waitFor();
  checks.push("Linked quiz displays automatic-completion hint; manual completion remains available");
  fs.mkdirSync(output, { recursive: true });
  for (const width of [1366, 768, 375]) {
    await page.setViewportSize({ width, height: 900 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `Horizontal overflow at ${width}px`);
    await page.screenshot({ path: path.join(output, `quiz-progress-before-${width}.png`), fullPage: true });
  }

  // Đi tới quiz rồi quay lại bằng Back: trang học có thể được Next.js khôi phục từ cache.
  await page.locator(`a[href="/quizzes/${quiz.id}"]`).first().click();
  await page.waitForURL(`**/quizzes/${quiz.id}`);
  const attempt = await api("POST", `/api/quizzes/${quiz.id}/attempts`, token);
  await api("POST", `/api/attempts/${attempt.id}/submit`, token, {
    answers: questions.map(question => ({ questionId: question.id,
      selectedOptionIds: question.options.filter(option => option.isCorrect).map(option => option.id) })),
  });
  await page.goBack();
  await page.getByText("Chúc mừng! Bạn đã hoàn thành khóa học.", { exact: true }).waitFor({ timeout: 20000 });
  await page.getByText("Đã hoàn thành", { exact: true }).last().waitFor();
  assert.equal(await page.getByRole("button", { name: "Đánh dấu hoàn thành", exact: true }).count(), 0);
  await page.locator(`a[href="/certificates/${enrollment.id}"]`).waitFor();
  checks.push("Pass via real quiz API, browser Back refreshes progress through Kafka, certificate link appears");

  for (const width of [1366, 768, 375]) {
    await page.setViewportSize({ width, height: 900 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `Horizontal overflow at ${width}px`);
    await page.screenshot({ path: path.join(output, `quiz-progress-after-${width}.png`), fullPage: true });
  }
  checks.push("Before/after layouts fit 1366, 768 and 375 pixels");
  assert.deepEqual(errors, []);
}

run().catch(error => { errors.push(error.message); process.exitCode = 1; }).finally(async () => {
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, "quiz-progress-browser.json"), JSON.stringify({
    sourceCommit: process.env.SOURCE_COMMIT || null, checkedAt: new Date().toISOString(),
    checks, errors, passed: errors.length === 0,
  }, null, 2) + "\n");
  if (browser) await browser.close();
  console.log(JSON.stringify({ checks, errors }));
});
