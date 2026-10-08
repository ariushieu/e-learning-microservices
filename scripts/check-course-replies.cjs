// Fixture riêng qua gateway và web production; kết quả không lưu token.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web = process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output = process.env.COURSE_UI_OUTPUT || path.join(require('node:os').tmpdir(), 'course-reply-check');
const password = process.env.COURSE_QA_PASSWORD || 'Test@123456';
const tag = 'replies-ui-' + Date.now(), checks = [], errors = [], writers = [];
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
async function enroll(token) {
  await retry(() => api('POST', '/api/enrollments', { courseId }, token, 201));
  await retry(async () => assert.equal((await api('GET', `/api/courses/${courseId}/reviews/me`, undefined, token)).canReview, true));
  writers.push(token);
}
async function openPage(token, suffix = '?reviewPage=2#reviews') {
  const context = await browser.newContext();
  if (token) await context.addCookies([{ name: 'el_access', value: token, url: web }]);
  const page = await context.newPage();
  page.on('pageerror', e => errors.push(e.message));
  await page.goto(`${web}/courses/${courseId}${suffix}`);
  await page.locator('#reviews article').first().waitFor();
  return page;
}
async function run() {
  teacher = await login('qa.instructor.a@example.com');
  const student = await login('qa.student@example.com');
  const other = await login('qa.instructor.b@example.com');
  const admin = await login(process.env.COURSE_ADMIN_EMAIL || 'admin@elearning.hunre.edu.vn',
    process.env.COURSE_ADMIN_PASSWORD || 'Admin@123456');
  const cat = await api('POST', '/api/categories', { name: tag, slug: tag }, teacher, 201);
  const course = await api('POST', '/api/courses', { title: 'Kiểm thử phản hồi ' + tag,
    slug: tag, categoryId: cat.id, level: 'BEGINNER', price: 0, language: 'vi' }, teacher, 201);
  courseId = course.id;
  await api('PATCH', `/api/courses/${courseId}/status`, { status: 'PUBLISHED' }, teacher);
  await enroll(student);
  const review = await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 5, comment: 'Nhận xét S cần phản hồi' }, student);
  for (let i = 0; i < 5; i++) {
    const email = `${tag}-${i}@example.com`;
    await api('POST', '/api/auth/register', { email, password, fullName: 'Học viên phân trang ' + i }, undefined, 201);
    const token = await login(email); await enroll(token);
    await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 3, comment: 'Nhận xét bổ sung ' + i }, token);
  }
  const replyPath = `/api/courses/${courseId}/reviews/${review.id}/reply`;
  const state = async () => (await api('GET', `/api/courses/${courseId}/reviews/me`, undefined, student)).review;
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  const design = await browser.newPage();
  await design.goto(web + '/design');
  await design.screenshot({ path: path.join(output, 'design.png'), fullPage: true });
  await design.close();
  for (const [role, token] of [['khách', null], ['học viên', student], ['giảng viên khác', other]]) {
    const page = await openPage(token);
    assert.equal(await page.getByRole('button', { name: /^(Trả lời|Sửa phản hồi|Xóa phản hồi)$/ }).count(), 0);
    checks.push(`${role} không có nút quản lý phản hồi`);
    await page.context().close();
  }
  const page = await openPage(teacher), section = page.locator('#reviews');
  await section.getByRole('button', { name: 'Trả lời', exact: true }).click();
  const input = () => section.getByRole('textbox', { name: 'Nội dung phản hồi' });
  await input().fill('   ');
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByText('Nhập phản hồi từ 1 đến 1000 ký tự, không chỉ gồm khoảng trắng.', { exact: true }).waitFor();
  assert.equal((await state()).reply, null);
  checks.push('Khoảng trắng bị chặn ở form, không lưu');
  const draft = 'Cảm ơn bạn đã góp ý về nội dung khóa học.';
  await input().fill(draft);
  const route = '**' + replyPath;
  await page.route(route, r => r.fulfill({ status: 503, contentType: 'application/json',
    body: JSON.stringify({ success: false, message: 'Máy chủ tạm thời không sẵn sàng', code: 'TEST_UNAVAILABLE' }) }));
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByRole('alert').filter({ hasText: 'Máy chủ tạm thời' }).waitFor();
  assert.equal(await input().inputValue(), draft);
  assert.equal((await state()).reply, null);
  checks.push('Lỗi lưu 503 giữ bản nháp và hiện thông báo');
  await page.unroute(route);
  await page.route(route, r => r.fulfill({ status: 400, contentType: 'application/json',
    body: JSON.stringify({ success: false, message: 'Dữ liệu không hợp lệ', code: 'VALIDATION_FAILED', fieldErrors: [{ field: 'content', message: 'Nội dung phản hồi không hợp lệ' }] }) }));
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByText('Nội dung phản hồi không hợp lệ', { exact: true }).waitFor();
  assert.equal(await input().getAttribute('aria-invalid'), 'true');
  assert.equal(await input().inputValue(), draft);
  checks.push('Lỗi backend theo ô content được hiển thị dưới ô nhập');
  await page.unroute(route);
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByText(draft, { exact: true }).waitFor();
  await section.getByRole('button', { name: 'Sửa phản hồi', exact: true }).waitFor();
  assert.equal(new URL(page.url()).searchParams.get('reviewPage'), '2');
  assert.equal((await state()).reply, draft);
  checks.push('A lưu phản hồi, cập nhật ngay và giữ trang 2');
  const guest = await openPage();
  await guest.locator('#reviews').getByText('Phản hồi của giảng viên', { exact: true }).waitFor();
  await guest.locator('#reviews').getByText(draft, { exact: true }).waitFor();
  checks.push('Khách thấy nhãn và nội dung phản hồi');
  await section.getByRole('button', { name: 'Sửa phản hồi', exact: true }).click();
  const html = '<img src=x onerror="window.replyInjected=true">';
  await input().fill(html);
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByText(html, { exact: true }).waitFor();
  await guest.reload(); await guest.locator('#reviews').getByText(html, { exact: true }).waitFor();
  assert.equal(await guest.evaluate(() => window.replyInjected), undefined);
  assert.equal(await guest.locator('#reviews img').count(), 0);
  checks.push('Sửa phản hồi có HTML: hiển thị nguyên văn, không thực thi');
  const savedAt = (await state()).repliedAt;
  await api('PUT', `/api/courses/${courseId}/reviews/me`, { rating: 2, comment: 'S đã sửa số sao' }, student);
  await page.reload(); await section.getByText(html, { exact: true }).waitFor();
  assert.equal((await state()).repliedAt, savedAt);
  const afterEdit = await api('GET', '/api/courses/' + courseId);
  assert.equal(afterEdit.ratingAvg, 2.83); assert.equal(afterEdit.ratingCount, 6);
  checks.push('S sửa số sao: giữ nguyên phản hồi/thời gian, tổng điểm khớp');
  const adminPage = await openPage(admin), adminSection = adminPage.locator('#reviews');
  await adminSection.getByRole('button', { name: 'Sửa phản hồi', exact: true }).click();
  await adminSection.getByRole('textbox', { name: 'Nội dung phản hồi' }).fill('Admin đã cập nhật phản hồi');
  await adminSection.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await adminSection.getByText('Admin đã cập nhật phản hồi', { exact: true }).waitFor();
  checks.push('Admin chưa ghi danh sửa được phản hồi qua giao diện');
  await page.reload();
  await section.getByRole('button', { name: 'Xóa phản hồi', exact: true }).click();
  await page.getByRole('button', { name: 'Giữ lại', exact: true }).click();
  assert.equal((await state()).reply, 'Admin đã cập nhật phản hồi');
  checks.push('Hủy xác nhận xóa giữ nguyên phản hồi');
  await page.route(route, r => r.fulfill({ status: 503, contentType: 'application/json',
    body: JSON.stringify({ success: false, message: 'Chưa xóa được phản hồi', code: 'TEST_UNAVAILABLE' }) }));
  await section.getByRole('button', { name: 'Xóa phản hồi', exact: true }).click();
  await page.getByRole('button', { name: 'Xác nhận xóa phản hồi', exact: true }).click();
  await page.getByRole('alertdialog').getByRole('alert').waitFor();
  assert.equal((await state()).reply, 'Admin đã cập nhật phản hồi');
  checks.push('Lỗi xóa giữ hộp xác nhận để thử lại');
  await page.unroute(route);
  await page.setViewportSize({ width: 375, height: 1000 });
  await page.screenshot({ path: path.join(output, 'reply-delete-375.png') });
  await page.getByRole('button', { name: 'Xác nhận xóa phản hồi', exact: true }).click();
  await section.getByRole('button', { name: 'Trả lời', exact: true }).waitFor();
  assert.equal((await state()).reply, null); assert.equal((await state()).repliedAt, null);
  assert.equal(await section.getByText('Phản hồi của giảng viên', { exact: true }).count(), 0);
  checks.push('Xóa chỉ phản hồi, giữ đánh giá và cho trả lời lại');
  await section.getByRole('button', { name: 'Trả lời', exact: true }).click();
  await input().fill('Cảm ơn bạn. ' + 'x'.repeat(980));
  for (const width of [375, 768, 1366]) {
    await page.setViewportSize({ width, height: 1000 });
    await input().scrollIntoViewIfNeeded();
    const bounds = await page.evaluate(() => ({ width: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
    assert.ok(bounds.scroll <= bounds.width + 1, JSON.stringify(bounds));
    await page.screenshot({ path: path.join(output, `reply-form-${width}.png`) });
    checks.push(`Form phản hồi không tràn ngang ở ${width}px`);
  }
  await section.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await section.getByRole('button', { name: 'Sửa phản hồi', exact: true }).waitFor();
  await page.setViewportSize({ width: 375, height: 1000 });
  await section.locator('article').scrollIntoViewIfNeeded();
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1));
  await page.screenshot({ path: path.join(output, 'reply-long-375.png') });
  checks.push('Phản hồi gần 1000 ký tự, từ dài không làm tràn điện thoại');
  await api('DELETE', `/api/courses/${courseId}/reviews/me`, undefined, student);
  await guest.goto(`${web}/courses/${courseId}`);
  await guest.locator('#reviews article').first().waitFor();
  assert.equal(await guest.getByText('Phản hồi của giảng viên', { exact: true }).count(), 0);
  checks.push('S xóa đánh giá: phản hồi không còn trên trang công khai');
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
    fs.writeFileSync(path.join(output, 'reply-ui-results.json'), JSON.stringify({ tag, courseId, checks, errors }, null, 2) + '\n');
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
