// Kiểm thử giao diện và API bằng fixture riêng; chỉ lưu bằng chứng không có token.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const base = process.env.COURSE_GATEWAY_URL || "http://localhost:8080";
const web = process.env.COURSE_WEB_URL || "http://127.0.0.1:3000";
const output = process.env.COURSE_UI_OUTPUT || path.join(require("node:os").tmpdir(), "course-resource-check");
fs.mkdirSync(output, { recursive: true });
const tag = "resources-ui-" + Date.now(),
  checks = [],
  errors = [],
  writers = [];
let browser, courseId, teacher;
const password = process.env.COURSE_QA_PASSWORD || "Test@123456";
async function api(method, url, body, token, expected = 200) {
  const r = await fetch(base + url, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: "Bearer " + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(10000),
  });
  assert.equal(r.status, expected, `${method} ${url}`);
  return (await r.json()).data;
}
async function login(email) {
  return (await api("POST", "/api/auth/login", { email, password })).accessToken;
}
async function retry(fn) {
  let error;
  for (let i = 0; i < 30; i++) {
    try {
      return await fn();
    } catch (e) {
      error = e;
      await new Promise((r) => setTimeout(r, 500));
    }
  }
  throw error;
}
async function run() {
  teacher = await login("qa.instructor.a@example.com");
  const student = await login("qa.student@example.com");
  const other = await login("qa.instructor.b@example.com");
  const cat = await api("POST", "/api/categories", { name: tag, slug: tag }, teacher, 201);
  const c = await api(
    "POST",
    "/api/courses",
    { title: "Tài liệu bài học " + tag, slug: tag, categoryId: cat.id, level: "BEGINNER", price: 0, language: "vi" },
    teacher,
    201,
  );
  courseId = c.id;
  const section = await api(
    "POST",
    `/api/courses/${courseId}/sections`,
    { title: "Chương tài liệu", position: 1 },
    teacher,
    201,
  );
  const lesson = await api(
    "POST",
    `/api/sections/${section.id}/lessons`,
    { title: "Bài tài liệu", type: "ARTICLE", content: "Nội dung thử tài liệu", isPreview: false, position: 1 },
    teacher,
    201,
  );
  const resourcePath = `/api/lessons/${lesson.id}/resources`;
  await api("PATCH", `/api/courses/${courseId}/status`, { status: "PUBLISHED" }, teacher);
  await retry(() => api("POST", "/api/enrollments", { courseId }, student, 201));
  await retry(async () =>
    assert.equal((await api("GET", `/api/courses/${courseId}/reviews/me`, undefined, student)).canReview, true),
  );
  for (const [token, code] of [
    [undefined, 401],
    [student, 403],
    [other, 403],
  ]) {
    await api("POST", resourcePath, { name: "Không có quyền", fileUrl: "https://example.com/a" }, token, code);
  }
  checks.push("API thêm tài liệu: thiếu token 401, học viên và giảng viên khác 403");
  browser = await chromium.launch({
    headless: true,
    ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}),
  });
  const context = await browser.newContext();
  await context.addCookies([{ name: "el_access", value: teacher, url: web }]);
  const page = await context.newPage();
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto(web + "/design");
  await page.getByRole("main").waitFor();
  await page.goto(`${web}/instructor/courses/${courseId}`);
  await page.getByRole("button", { name: "Thao tác với bài Bài tài liệu", exact: true }).click();
  await page.getByRole("menuitem", { name: "Sửa bài học", exact: true }).click();
  const dialog = page.getByRole("dialog");
  const resourceForm = dialog.getByRole("region", { name: "Tài liệu đính kèm" });
  await resourceForm.getByText("Chưa có tài liệu đính kèm", { exact: true }).waitFor();
  checks.push("Form sửa bài có mục tài liệu và trạng thái rỗng");
  const name = resourceForm.getByRole("textbox", { name: "Tên tài liệu" });
  const url = resourceForm.getByRole("textbox", { name: "URL tài liệu" });
  await name.fill("Tài liệu kiểm thử");
  await url.fill("javascript:alert(1)");
  const invalid = page.waitForResponse((r) => r.url().endsWith(resourcePath) && r.request().method() === "POST");
  await resourceForm.getByRole("button", { name: "Thêm tài liệu", exact: true }).click();
  assert.equal((await invalid).status(), 400);
  await resourceForm.getByText("URL tài liệu phải là địa chỉ http:// hoặc https:// hợp lệ", { exact: true }).waitFor();
  assert.equal(await url.inputValue(), "javascript:alert(1)");
  checks.push("javascript bị API chặn 400, lỗi dưới ô URL, giữ dữ liệu nhập");
  const target = "https://example.com/course-resource.pdf?source=" + tag;
  await url.fill(target);
  await page.route("**" + resourcePath, (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ success: false, message: "Dịch vụ tạm thời không sẵn sàng" }),
    }),
  );
  await resourceForm.getByRole("button", { name: "Thêm tài liệu", exact: true }).click();
  await resourceForm.getByText("Dịch vụ tạm thời không sẵn sàng", { exact: true }).waitFor();
  assert.equal(await name.inputValue(), "Tài liệu kiểm thử");
  assert.equal(await url.inputValue(), target);
  await page.unroute("**" + resourcePath);
  checks.push("Lỗi khi thêm giữ tên và URL, cho phép thử lại");
  await resourceForm.getByRole("button", { name: "Thêm tài liệu", exact: true }).click();
  const link = resourceForm.getByRole("link", { name: "Tài liệu kiểm thử" });
  await link.waitFor();
  assert.equal(await link.getAttribute("href"), target);
  assert.equal(await name.inputValue(), "");
  const saved = await api("GET", `/api/lessons/${lesson.id}`, undefined, teacher);
  assert.equal(saved.resources.length, 1);
  assert.equal(saved.title, "Bài tài liệu");
  const rid = saved.resources[0].id;
  checks.push("Thêm HTTPS hiển thị ngay, không gửi hoặc thay đổi form bài học");
  for (const [token, code] of [
    [undefined, 401],
    [student, 403],
    [other, 403],
  ])
    await api("DELETE", resourcePath + "/" + rid, undefined, token, code);
  const another = await api(
    "POST",
    `/api/sections/${section.id}/lessons`,
    { title: "Bài khác", type: "ARTICLE", content: "Khác" },
    teacher,
    201,
  );
  await api("DELETE", `/api/lessons/${another.id}/resources/${rid}`, undefined, teacher, 404);
  checks.push("API xóa chặn sai quyền, sai lessonId 404, giữ nguyên tài liệu");
  const denied = await api("GET", `/api/lessons/${lesson.id}`, undefined, other);
  assert.deepEqual(denied.resources, []);
  checks.push("Người chưa ghi danh không thấy tài liệu bài thường");
  const sc = await browser.newContext();
  await sc.addCookies([{ name: "el_access", value: student, url: web }]);
  const sp = await sc.newPage();
  sp.on("pageerror", (e) => errors.push(e.message));
  await sp.goto(`${web}/learn/${courseId}?lesson=${lesson.id}`);
  const learnerLink = sp
    .getByRole("listitem")
    .filter({ hasText: "Tài liệu kiểm thử" })
    .getByRole("link", { name: "Tải về", exact: true });
  await learnerLink.waitFor();
  assert.equal(await learnerLink.getAttribute("href"), target);
  // Chặn mạng của tài liệu ngoài; vẫn kiểm được tab mới mở đúng URL thật.
  await sc.route("https://example.com/**", (r) =>
    r.fulfill({ status: 200, contentType: "text/plain", body: "Tài liệu kiểm thử" }),
  );
  const popupPromise = sp.waitForEvent("popup");
  await learnerLink.click();
  const popup = await popupPromise;
  await popup.waitForLoadState();
  assert.equal(popup.url(), target);
  await popup.close();
  checks.push("Học viên đã ghi danh thấy link, bấm mở đúng URL ở tab mới");
  for (const width of [375, 768, 1366]) {
    await page.setViewportSize({ width, height: 1000 });
    await resourceForm.scrollIntoViewIfNeeded();
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
    assert.ok(await dialog.evaluate((e) => e.scrollWidth <= e.clientWidth + 1));
    await page.screenshot({ path: path.join(output, `resources-${width}.png`) });
    checks.push(`Form tài liệu không tràn ở ${width}px`);
  }
  await resourceForm.getByRole("button", { name: "Xóa tài liệu Tài liệu kiểm thử", exact: true }).click();
  await page.getByRole("button", { name: "Giữ lại", exact: true }).click();
  assert.equal((await api("GET", `/api/lessons/${lesson.id}`, undefined, teacher)).resources.length, 1);
  checks.push("Giữ lại trong hộp xác nhận không xóa dữ liệu");
  await resourceForm.getByRole("button", { name: "Xóa tài liệu Tài liệu kiểm thử", exact: true }).click();
  await page.route("**" + resourcePath + "/" + rid, (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ success: false, message: "Chưa xóa được tài liệu" }),
    }),
  );
  await page.getByRole("button", { name: "Xác nhận xóa tài liệu", exact: true }).click();
  await page.getByRole("alertdialog").getByText("Chưa xóa được tài liệu", { exact: true }).waitFor();
  await page.unroute("**" + resourcePath + "/" + rid);
  checks.push("Xóa lỗi giữ hộp xác nhận và hiện thông báo để thử lại");
  await page.getByRole("button", { name: "Xác nhận xóa tài liệu", exact: true }).click();
  await resourceForm.getByText("Chưa có tài liệu đính kèm", { exact: true }).waitFor();
  await sp.reload();
  assert.equal(await sp.locator("a").filter({ hasText: "Tải về" }).count(), 0);
  checks.push("Xóa thành công cập nhật form và trang học không còn link");
  // Ghi danh giảng viên B để tạo trung bình đúng 4,5 bằng dữ liệu thật.
  await retry(() => api("POST", "/api/enrollments", { courseId }, other, 201));
  await retry(async () =>
    assert.equal((await api("GET", `/api/courses/${courseId}/reviews/me`, undefined, other)).canReview, true),
  );
  writers.push(student, other);
  await api("PUT", `/api/courses/${courseId}/reviews/me`, { rating: 5 }, student);
  await api("PUT", `/api/courses/${courseId}/reviews/me`, { rating: 4 }, other);
  await sp.goto(`${web}/courses/${courseId}`);
  const stars = sp.locator("#reviews").getByRole("img", { name: "4,5 trên 5 sao", exact: true });
  await stars.waitFor();
  assert.deepEqual(await stars.locator("span[style]").evaluateAll((es) => es.map((e) => e.style.width)), [
    "100%",
    "100%",
    "100%",
    "100%",
    "50%",
  ]);
  await stars.scrollIntoViewIfNeeded();
  await sp.screenshot({ path: path.join(output, "half-stars.png") });
  checks.push("Trung bình thật 4,5 hiển thị bốn sao đầy và 50% sao cuối");
  assert.deepEqual(errors, []);
  checks.push("Không có lỗi JavaScript");
}
(async () => {
  try {
    await run();
  } catch (e) {
    errors.push(e.message);
    console.error(e);
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
    if (courseId) {
      for (const token of writers) {
        try {
          await api("DELETE", `/api/courses/${courseId}/reviews/me`, undefined, token);
        } catch (e) {
          errors.push("Cleanup: " + e.message);
          process.exitCode = 1;
        }
      }
      try {
        await api("PATCH", `/api/courses/${courseId}/status`, { status: "ARCHIVED" }, teacher);
      } catch (e) {
        errors.push("Archive: " + e.message);
        process.exitCode = 1;
      }
    }
    fs.writeFileSync(
      path.join(output, "resource-ui-results.json"),
      JSON.stringify({ tag, courseId, checks, errors }, null, 2),
    );
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
