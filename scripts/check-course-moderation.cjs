// Chạy qua gateway và web production, dùng fixture riêng; bằng chứng không lưu token.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web = process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output = process.env.COURSE_UI_OUTPUT || path.join(require('node:os').tmpdir(), 'course-moderation-check');
const password = process.env.COURSE_QA_PASSWORD || 'Test@123456';
const tag = 'moderation-ui-' + Date.now(), checks = [], errors = [], writers = [];
let browser, courseId, teacher;
fs.mkdirSync(output, { recursive: true });
async function api(method, url, body, token, expected = 200) {
  const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json',
    ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  assert.equal(r.status, expected, `${method} ${url}`);
  return (await r.json()).data;
}
async function login(email, pass = password) {
  return (await api('POST', '/api/auth/login', { email, password: pass })).accessToken;
}
async function retry(fn) {
  let error;
  for (let i = 0; i < 30; i++) {
    try { return await fn(); } catch (e) { error = e; await new Promise(r => setTimeout(r, 500)); }
  }
  throw error;
}
async function stats(count, avg) {
  const c = await api('GET', '/api/courses/' + courseId);
  assert.equal(c.ratingCount, count); assert.equal(c.ratingAvg, avg);
}
async function enroll(token) {
  await retry(() => api('POST', '/api/enrollments', { courseId }, token, 201));
  await retry(async () => assert.equal((await api('GET', `/api/courses/${courseId}/reviews/me`, undefined, token)).canReview, true));
  writers.push(token);
}
async function openPage(token) {
  const context = await browser.newContext();
  if (token) await context.addCookies([{ name: 'el_access', value: token, url: web }]);
  const page = await context.newPage();
  page.on('pageerror', e => errors.push(e.message));
  await page.goto(`${web}/courses/${courseId}`);
  await page.locator('#reviews article').first().waitFor();
  return page;
}
async function run() {
  teacher = await login('qa.instructor.a@example.com');
  const student = await login('qa.student@example.com');
  await api('POST', '/api/auth/register', { email: tag + '@example.com', password, fullName: 'Học viên B kiểm thử gỡ' }, undefined, 201);
  const other = await login(tag + '@example.com');
  const admin = await login(process.env.COURSE_ADMIN_EMAIL || 'admin@elearning.hunre.edu.vn',
    process.env.COURSE_ADMIN_PASSWORD || 'Admin@123456');
  const cat = await api('POST', '/api/categories', { name: tag, slug: tag }, teacher, 201);
  const course = await api('POST', '/api/courses', { title: 'Kiểm thử gỡ đánh giá ' + tag,
    slug: tag, categoryId: cat.id, level: 'BEGINNER', price: 0, language: 'vi' }, teacher, 201);
  courseId = course.id;
  await api('PATCH', `/api/courses/${courseId}/status`, { status: 'PUBLISHED' }, teacher);
  await enroll(student); await enroll(other);
  await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 5, comment: 'Nhận xét S giữ lại' }, student);
  const b = await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 1, comment: 'Nhận xét B cần gỡ' }, other);
  await stats(2, 3);
  checks.push('S 5 sao và B 1 sao: trung bình 3, hai đánh giá');
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  for (const [role, token] of [['khách', null], ['S', student], ['B', other], ['chủ khóa', teacher]]) {
    const page = await openPage(token);
    assert.equal(await page.getByRole('button', { name: /^Gỡ đánh giá của/ }).count(), 0);
    checks.push(`${role} không thấy nút Gỡ`);
    await page.context().close();
  }
  const page = await openPage(admin), section = page.locator('#reviews');
  assert.equal(await section.getByRole('button', { name: /^Gỡ đánh giá của/ }).count(), 2);
  checks.push('Admin thấy nút Gỡ trên từng nhận xét');
  const removeB = () => section.locator('article').filter({ hasText: 'Nhận xét B cần gỡ' }).getByRole('button', { name: /^Gỡ/ });
  await removeB().click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Giữ lại', exact: true }).click();
  await stats(2, 3);
  assert.equal(await section.locator('article').count(), 2);
  checks.push('Hủy xác nhận giữ nguyên đánh giá và điểm');
  const route = `**/api/courses/${courseId}/reviews/${b.id}`;
  await page.route(route, r => r.fulfill({ status: 503, contentType: 'application/json',
    body: JSON.stringify({ success: false, message: 'Máy chủ tạm thời không sẵn sàng', code: 'TEST_UNAVAILABLE' }) }));
  await removeB().click();
  await page.getByRole('button', { name: 'Xác nhận gỡ', exact: true }).click();
  await page.getByRole('alertdialog').getByRole('alert').waitFor();
  await stats(2, 3);
  checks.push('API lỗi: hiện thông báo trong hộp thoại, không xóa hoặc đổi điểm');
  await page.unroute(route);
  await page.setViewportSize({ width: 375, height: 1000 });
  await page.screenshot({ path: path.join(output, 'moderation-confirm-375.png') });
  await page.getByRole('button', { name: 'Xác nhận gỡ', exact: true }).click();
  await retry(async () => { await stats(1, 5); assert.equal(await section.locator('article').count(), 1); });
  await section.getByText('1 đánh giá · Từ người đã ghi danh', { exact: true }).waitFor();
  await section.getByLabel('5 trên 5 sao', { exact: true }).first().waitFor();
  assert.equal(await section.getByText('Nhận xét B cần gỡ', { exact: true }).count(), 0);
  checks.push('Gỡ B thành công: danh sách và tổng điểm cập nhật ngay thành 5/1');
  const bPage = await openPage(other), bSection = bPage.locator('#reviews');
  await bSection.getByRole('button', { name: 'Gửi đánh giá', exact: true }).waitFor();
  await bSection.locator('label').filter({ has: bPage.locator('input[name="rating"][value="4"]') }).click();
  await bPage.getByRole('textbox', { name: 'Nhận xét (không bắt buộc)' }).fill('B đã viết lại');
  await bSection.getByRole('button', { name: 'Gửi đánh giá', exact: true }).click();
  await bSection.getByRole('button', { name: 'Lưu thay đổi', exact: true }).waitFor();
  await retry(() => stats(2, 4.5));
  const rewritten = await api('GET', `/api/courses/${courseId}/reviews/me`, undefined, other);
  assert.notEqual(rewritten.review.id, b.id);
  checks.push('B viết lại qua form thành bản ghi mới, điểm 4,5/2');
  for (let i = 0; i < 4; i++) {
    const email = `${tag}-${i}@example.com`;
    await api('POST', '/api/auth/register', { email, password, fullName: 'Học viên phân trang ' + i }, undefined, 201);
    const token = await login(email); await enroll(token);
    await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 3, comment: 'Nhận xét bổ sung ' + i }, token);
  }
  await page.goto(`${web}/courses/${courseId}?reviewPage=2#reviews`);
  await section.locator('article').getByText('Nhận xét S giữ lại', { exact: true }).waitFor();
  assert.equal(await section.locator('article').count(), 1);
  await section.getByRole('button', { name: /^Gỡ đánh giá của/ }).click();
  await page.getByRole('button', { name: 'Xác nhận gỡ', exact: true }).click();
  await page.waitForURL(u => !u.searchParams.has('reviewPage'));
  await retry(async () => { await stats(5, 3.2); assert.equal(await section.locator('article').count(), 5); });
  await section.getByText('5 đánh giá · Từ người đã ghi danh', { exact: true }).waitFor();
  checks.push('Gỡ nhận xét cuối trang 2: về trang đầu, không kẹt trang rỗng; điểm 3,2/5');
  for (const width of [375, 768, 1366]) {
    await page.setViewportSize({ width, height: 1000 }); await section.scrollIntoViewIfNeeded();
    const bounds = await page.evaluate(() => ({ width: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
    assert.ok(bounds.scroll <= bounds.width + 1, JSON.stringify(bounds));
    await page.screenshot({ path: path.join(output, `moderation-${width}.png`) });
    checks.push(`Nút Gỡ và danh sách không tràn ngang tại ${width}px`);
  }
  assert.deepEqual(errors, []); checks.push('Không có lỗi JavaScript');
}
(async () => {
  try { await run(); } catch (e) { errors.push(e.message); console.error(e); process.exitCode = 1; }
  finally {
    if (browser) await browser.close();
    if (courseId) {
      for (const token of writers) {
        try {
          const mine = await api('GET', `/api/courses/${courseId}/reviews/me`, undefined, token);
          if (mine.review) await api('DELETE', `/api/courses/${courseId}/reviews/me`, undefined, token);
        } catch (e) { errors.push('Cleanup: ' + e.message); process.exitCode = 1; }
      }
      try { await api('PATCH', `/api/courses/${courseId}/status`, { status: 'ARCHIVED' }, teacher); }
      catch (e) { errors.push('Archive: ' + e.message); process.exitCode = 1; }
    }
    fs.writeFileSync(path.join(output, 'moderation-ui-results.json'), JSON.stringify({ tag, courseId, checks, errors }, null, 2) + '\n');
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
