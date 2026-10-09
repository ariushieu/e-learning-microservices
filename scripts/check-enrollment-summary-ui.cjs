// Runs against the disposable Docker stack after the enrollment collection.
// Log in through the API: Newman exports the original collection, not runtime variables.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require(process.env.ENROLLMENT_PLAYWRIGHT_MODULE || "playwright");
const web = process.env.ENROLLMENT_WEB_URL || "http://localhost:3000";
const gateway = process.env.ENROLLMENT_GATEWAY_URL || "http://localhost:8080";
const output = process.env.ENROLLMENT_UI_OUTPUT || path.join(require("node:os").tmpdir(), "enrollment-summary-ui");
const variables = {};
let courseId, summaryPath;
const checks = [], errors = [];
let browser;

async function api(method, url, body) {
  const response = await fetch(gateway + url, {
    method, headers: { ...(variables.tokenA ? { Authorization: `Bearer ${variables.tokenA}` } : {}), "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000),
  });
  assert.ok(response.ok, `${method} ${url}: HTTP ${response.status}`);
  return (await response.json()).data;
}

async function run() {
  const auth = await api("POST", "/api/auth/login", { email: "qa.instructor.a@example.com", password: "Test@123456" });
  variables.tokenA = auth.accessToken;
  const courses = await api("GET", `/api/courses?instructorId=${auth.user.id}&keyword=${encodeURIComponent("Thống kê học tập")}&size=100`);
  assert.equal(courses.totalElements, 1, "Expected exactly one summary fixture in this disposable stack");
  courseId = courses.content[0].id;
  summaryPath = `/api/courses/${courseId}/learners/summary`;
  const curriculum = await api("GET", `/api/courses/${courseId}/curriculum`);
  assert.equal(curriculum.length, 1);
  assert.equal(curriculum[0].title, "Chương thống kê");
  assert.equal(curriculum[0].lessons.length, 2);
  variables.summarySectionId = curriculum[0].id;
  variables.summaryLesson2Id = curriculum[0].lessons.find((lesson) => lesson.title === "Bài thống kê 2")?.id;
  assert.ok(variables.summaryLesson2Id, "Expected the collection's second lesson");
  const completed = await api("GET", `/api/courses/${courseId}/learners?status=COMPLETED`);
  assert.equal(completed.totalElements, 1);
  variables.studentName = completed.content[0].learnerName;
  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1366, height: 1000 } });
  await context.addCookies([{ name: "el_access", value: variables.tokenA, url: web }]);
  const page = await context.newPage();
  page.on("pageerror", (error) => errors.push(error.message));
  let summaryRequests = 0;
  page.on("request", (request) => { if (new URL(request.url()).pathname === summaryPath) summaryRequests++; });
  await page.goto(web + "/design");
  await page.getByRole("main").waitFor();
  // Students live in their own tab; ?tab= keeps it open across reloads.
  await page.goto(`${web}/instructor/courses/${courseId}?tab=hoc-vien`);
  const section = page.locator("#hoc-vien");
  const summary = section.locator('[aria-label="Số liệu học tập toàn khóa"]');
  const lesson1 = () => summary.getByRole("row").filter({ hasText: "Bài thống kê 1" });
  const lesson2 = () => summary.getByRole("row").filter({ hasText: "Bài thống kê 2" });
  await lesson2().getByText("33,33%", { exact: true }).waitFor();
  for (const [label, value] of [["Đang học", "2"], ["Đã hoàn thành", "1"], ["Tiến độ trung bình", "50%"], ["Tỉ lệ hoàn thành", "33,33%"]]) {
    await summary.getByText(label, { exact: true }).locator("..").getByText(value, { exact: true }).waitFor();
  }
  await lesson1().getByText("66,67%", { exact: true }).waitFor();
  await lesson2().getByText("Giảm 33,34 điểm % so với bài trước", { exact: true }).waitFor();
  checks.push("Real Docker fixture: four stats, precise lesson rates and drop label");

  const beforeFilter = summaryRequests;
  const filtered = page.waitForResponse((r) => r.url().includes(`/courses/${courseId}/learners?`) && r.url().includes("status=COMPLETED"));
  await section.getByLabel("Trạng thái học").selectOption("COMPLETED");
  assert.equal((await filtered).status(), 200);
  await section.getByRole("table").last().getByText(variables.studentName, { exact: true }).waitFor();
  assert.equal(summaryRequests, beforeFilter);
  await lesson1().getByText("66,67%", { exact: true }).waitFor();
  checks.push("Roster status filter leaves whole-course summary unchanged without another summary request");

  let release;
  const held = new Promise((resolve) => { release = resolve; });
  await page.route("**" + summaryPath, async (route) => { await held; await route.continue(); });
  await section.getByRole("button", { name: "Làm mới", exact: true }).click();
  await section.getByRole("status", { name: "Đang tải số liệu học tập" }).waitFor();
  release();
  await lesson2().getByText("33,33%", { exact: true }).waitFor();
  await page.unroute("**" + summaryPath);
  checks.push("Refresh displays summary skeleton and reloads both datasets");

  await page.route("**" + summaryPath, (route) => route.fulfill({ status: 503, contentType: "application/json",
    body: JSON.stringify({ success: false, message: "Lỗi thống kê kiểm thử" }) }));
  await section.getByRole("button", { name: "Làm mới", exact: true }).click();
  await section.getByText("Không tải được số liệu học tập", { exact: true }).waitFor();
  await section.getByRole("table").last().getByText(variables.studentName, { exact: true }).waitFor();
  await page.unroute("**" + summaryPath);
  await section.getByRole("button", { name: "Làm mới", exact: true }).click();
  await lesson2().getByText("33,33%", { exact: true }).waitFor();
  checks.push("Summary failure preserves roster and refresh recovers it");

  // Add and then remove only this run's curriculum fixture. Existing progress remains untouched.
  const longTitle = "Bài chưa học " + "TênBàiRấtDài".repeat(10);
  await api("POST", `/api/sections/${variables.summarySectionId}/lessons`,
    { title: longTitle, type: "ARTICLE", content: "Fixture UI", position: 3 });
  await page.reload();
  const untouched = summary.getByRole("row").filter({ hasText: longTitle });
  await untouched.getByText("0%", { exact: true }).waitFor();
  checks.push("New untouched lesson is visible at zero without an aggregate progress record");

  for (const width of [375, 768, 1366]) {
    // Fit the section vertically so the sticky app header stays outside its screenshot.
    await page.setViewportSize({ width, height: 2200 });
    await section.scrollIntoViewIfNeeded();
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `Page overflow at ${width}`);
    assert.ok(await section.evaluate((element) => element.scrollWidth <= element.clientWidth + 1), `Summary overflow at ${width}`);
    await section.screenshot({ path: path.join(output, `summary-${width}.png`) });
    checks.push(`No horizontal overflow at ${width}px, including a long lesson title`);
  }
  const refresh = section.getByRole("button", { name: "Làm mới", exact: true });
  await refresh.focus();
  await page.keyboard.press("Tab");
  assert.ok(await section.getByLabel("Trạng thái học").evaluate((element) => document.activeElement === element));
  await page.keyboard.press("Tab");
  assert.ok(await section.getByLabel("Sắp xếp học viên").evaluate((element) => document.activeElement === element));
  checks.push("Keyboard Tab reaches roster controls from Refresh");

  await api("DELETE", `/api/lessons/${variables.summaryLesson2Id}`);
  await page.reload();
  await summary.getByRole("row").filter({ hasText: longTitle }).waitFor();
  assert.equal(await lesson2().count(), 0);
  const rawSummary = await api("GET", summaryPath);
  assert.ok(rawSummary.lessons.some((row) => row.lessonId === Number(variables.summaryLesson2Id)));
  checks.push("Deleted curriculum lesson stays hidden even when its historical aggregate is present");
  assert.deepEqual(errors, []);
}

(async () => {
  fs.mkdirSync(output, { recursive: true });
  try { await run(); }
  catch (error) { errors.push(error.message); console.error(error.message); process.exitCode = 1; }
  finally {
    if (browser) await browser.close();
    fs.writeFileSync(path.join(output, "results.json"), JSON.stringify({ checks, errors }, null, 2));
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
