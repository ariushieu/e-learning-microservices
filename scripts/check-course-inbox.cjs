// Fixture riêng, API thật qua gateway, web production; không lưu token vào bằng chứng.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web = process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output = process.env.COURSE_UI_OUTPUT || path.join(require('node:os').tmpdir(), 'course-inbox-check');
const password = process.env.COURSE_QA_PASSWORD || 'Test@123456';
const tag = 'inbox-ui-' + Date.now(), checks = [], errors = [], fixtures = [], pages = [];
let browser, teacher, admin;
fs.mkdirSync(output, { recursive: true });
async function api(method, url, body, token, expected = 200) {
  const response = await fetch(base + url, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000),
  });
  assert.equal(response.status, expected, `${method} ${url}`);
  return (await response.json()).data;
}
async function login(email, pass = password) {
  return (await api('POST', '/api/auth/login', { email, password: pass })).accessToken;
}
async function account(name, instructor = false) {
  const email = `${tag}-${name}@example.com`;
  await api('POST', '/api/auth/register', { email, password, fullName: 'Học viên ' + name }, null, 201);
  const auth = await api('POST', '/api/auth/login', { email, password });
  if (!instructor) return auth.accessToken;
  await api('PATCH', `/api/users/${auth.user.id}/roles`, { roles: ['ROLE_STUDENT', 'ROLE_INSTRUCTOR'] }, admin);
  return login(email);
}
async function retry(fn) {
  let error;
  for (let n = 0; n < 30; n++) {
    try { return await fn(); } catch (e) { error = e; await new Promise(r => setTimeout(r, 500)); }
  }
  throw error;
}
async function review(course, token, comment) {
  await retry(() => api('POST', '/api/enrollments', { courseId: course.id }, token, 201));
  await retry(async () => assert.equal((await api('GET', `/api/courses/${course.id}/reviews/me`, undefined, token)).canReview, true));
  const result = await api('PUT', `/api/courses/${course.id}/reviews/me`, { rating: 5, comment }, token);
  course.writers.push(token);
  return result;
}
async function open(token, url = '/instructor/reviews') {
  const context = await browser.newContext({ viewport: { width: 1366, height: 1000 } });
  if (token) await context.addCookies([{ name: 'el_access', value: token, url: web }]);
  const page = await context.newPage();
  page.on('pageerror', e => errors.push(e.message));
  pages.push(page);
  await page.goto(web + url);
  return page;
}
async function counts(page, scoped, global = scoped) {
  await retry(async () => assert.equal(await page.getByTestId('inbox-unreplied-count').innerText(), String(scoped)));
  await page.getByLabel(`${global} đánh giá chờ phản hồi`, { exact: true }).waitFor();
}
async function filter(page, status, courseId = '') {
  await page.getByLabel('Trạng thái', { exact: true }).selectOption(status);
  await page.getByLabel('Khóa học', { exact: true }).selectOption(String(courseId));
  await page.getByRole('button', { name: 'Áp dụng', exact: true }).click();
  await page.waitForURL(url => url.searchParams.get('replied') === status && url.searchParams.get('courseId') === String(courseId));
}
async function run() {
  admin = await login(process.env.COURSE_ADMIN_EMAIL || 'admin@elearning.hunre.edu.vn', process.env.COURSE_ADMIN_PASSWORD || 'Admin@123456');
  teacher = await account('A', true);
  const other = await account('B', true), student = await account('S');
  const cat = await api('POST', '/api/categories', { name: tag, slug: tag }, teacher, 201);
  for (let i = 1; i <= 2; i++) {
    const course = await api('POST', '/api/courses', { title: i === 1 ? 'Thiết kế hệ thống bền vững' : 'Kiến trúc phần mềm hiện đại',
      slug: `${tag}-${i}`, categoryId: cat.id, level: 'BEGINNER', price: 0, language: 'vi' }, teacher, 201);
    const fixture = { id: course.id, writers: [] }; fixtures.push(fixture);
    await api('PATCH', `/api/courses/${course.id}/status`, { status: 'PUBLISHED' }, teacher);
    fixture.review = await review(fixture, student, `Nhận xét khóa ${i}: Nội dung dễ hiểu, mong thầy bổ sung ví dụ thực tế.`);
  }
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  const page = await open(teacher), first = fixtures[0], second = fixtures[1];
  await counts(page, 2);
  assert.equal(await page.locator('article').count(), 2);
  await page.locator('article').first().getByText(/Nhận xét khóa 2/).waitFor();
  checks.push('A có hai khóa: hai thẻ mới nhất trước, số chờ và sidebar đều 2');
  await filter(page, 'false', first.id); await counts(page, 1, 2);
  assert.equal(await page.locator('article').count(), 1);
  checks.push('Lọc khóa: số tại trang theo khóa, sidebar vẫn tổng số 2');
  await filter(page, 'false'); await counts(page, 2);
  const card = () => page.locator('article').filter({ hasText: 'Nhận xét khóa 1:' });
  await card().getByRole('button', { name: 'Trả lời', exact: true }).click();
  const input = () => card().getByRole('textbox', { name: 'Nội dung phản hồi' });
  await input().fill('   ');
  await card().getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  const validation = 'Nhập phản hồi từ 1 đến 1000 ký tự, không chỉ gồm khoảng trắng.';
  await card().getByText(validation, { exact: true }).waitFor();
  const draft = 'Cảm ơn bạn, mình sẽ bổ sung ví dụ ở bài học tiếp theo.';
  await input().fill(draft);
  assert.equal(await card().getByText(validation, { exact: true }).count(), 0);
  await card().getByText(`${draft.length}/1000 ký tự`, { exact: true }).waitFor();
  assert.equal(await input().getAttribute('aria-invalid'), 'false');
  checks.push('Sửa lỗi khoảng trắng: lỗi biến mất và bộ đếm trở lại ngay trước khi Lưu');
  const route = `**/api/courses/${first.id}/reviews/${first.review.id}/reply`;
  await page.route(route, r => r.fulfill({ status: 503, contentType: 'application/json',
    body: JSON.stringify({ success: false, code: 'TEST_UNAVAILABLE', message: 'Chưa lưu được phản hồi' }) }));
  await card().getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await card().getByRole('alert').waitFor();
  assert.equal(await input().inputValue(), draft); await counts(page, 2);
  checks.push('API lưu lỗi 503: giữ bản nháp và số chờ');
  await page.unroute(route);
  for (const width of [375, 768, 1366]) {
    await page.setViewportSize({ width, height: 1000 });
    await input().scrollIntoViewIfNeeded();
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1));
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({ path: path.join(output, `inbox-form-${width}.png`), fullPage: true });
    checks.push(`Trang và form không tràn ngang ở ${width}px`);
  }
  await input().fill(draft + ' Cảm ơn.');
  await card().getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await counts(page, 1);
  assert.equal(await card().count(), 0);
  checks.push('Lưu thành công: thẻ rời bộ lọc chưa trả lời, trang và sidebar cập nhật 1');
  await page.goto(web + '/instructor');
  const stat = page.locator('a[href="/instructor/reviews"]').filter({ hasText: 'Đánh giá chờ phản hồi' });
  await stat.waitFor();
  await stat.getByText('1', { exact: true }).waitFor();
  await stat.click(); await counts(page, 1);
  checks.push('Tổng quan có thẻ đúng số 1 và liên kết đến trang đánh giá');
  await filter(page, 'true'); await counts(page, 1);
  assert.equal(await page.locator('article').count(), 1);
  await card().getByText('Phản hồi của giảng viên', { exact: true }).waitFor();
  checks.push('Bộ lọc đã trả lời hiện đúng thẻ và nhãn giảng viên');
  await api('PUT', `/api/courses/${first.id}/reviews/${first.review.id}/reply`, { content: '<img src=x onerror="window.inboxInjected=true">' }, admin);
  await page.reload();
  await card().getByText('Phản hồi của quản trị viên', { exact: true }).waitFor();
  assert.equal(await card().getByText('Phản hồi của giảng viên', { exact: true }).count(), 0);
  const guest = await open(null, `/courses/${first.id}`);
  await guest.locator('#reviews').getByText('Phản hồi của quản trị viên', { exact: true }).waitFor();
  assert.equal(await guest.evaluate(() => window.inboxInjected), undefined);
  await guest.locator('#reviews').getByText('<img src=x onerror="window.inboxInjected=true">', { exact: true }).waitFor();
  checks.push('Admin trả lời: nhãn quản trị đúng ở inbox và trang công khai; HTML chỉ là chữ');
  await card().getByRole('button', { name: 'Xóa phản hồi', exact: true }).click();
  await page.getByRole('button', { name: 'Giữ lại', exact: true }).click();
  await counts(page, 1); assert.equal(await card().count(), 1);
  checks.push('Hủy xác nhận xóa giữ nguyên thẻ và số chờ');
  await card().getByRole('button', { name: 'Xóa phản hồi', exact: true }).click();
  await page.getByRole('button', { name: 'Xác nhận xóa phản hồi', exact: true }).click();
  await counts(page, 2); assert.equal(await page.locator('article').count(), 0);
  await page.getByText('Chưa có đánh giá phù hợp', { exact: true }).waitFor();
  checks.push('Xóa phản hồi: bộ lọc đã trả lời rỗng và số chờ/sidebar trở về 2');
  await filter(page, 'all'); assert.equal(await page.locator('article').count(), 2);
  checks.push('Bộ lọc tất cả hiển thị hai đánh giá');
  const pageB = await open(other); await counts(pageB, 0);
  assert.equal(await pageB.locator('article').count(), 0);
  assert.equal(await pageB.getByLabel('Khóa học', { exact: true }).locator('option').count(), 1);
  await pageB.goto(`${web}/instructor/reviews?courseId=${first.id}`);
  await pageB.getByRole('alert').filter({ hasText: 'Không tải được đánh giá' }).waitFor();
  assert.equal(await pageB.locator('article').count(), 0);
  checks.push('B không thấy đánh giá/tên khóa A; nhập courseId của A hiện lỗi quyền');
  const pageS = await open(student); await pageS.waitForURL('**/khong-co-quyen');
  const noLogin = await open(null); await noLogin.waitForURL('**/login?**');
  checks.push('Học viên bị chuyển trang không có quyền, khách phải đăng nhập');
  const pageAdmin = await open(admin, `/instructor/reviews?courseId=${first.id}`);
  await pageAdmin.locator('article').filter({ hasText: 'Nhận xét khóa 1:' }).waitFor();
  checks.push('Admin xem được đánh giá khóa của A trên giao diện');
  await page.goto(`${web}/instructor/reviews?replied=bad`);
  await page.getByRole('alert').filter({ hasText: 'Bộ lọc hoặc số trang không hợp lệ.' }).waitFor();
  checks.push('URL bộ lọc không hợp lệ hiện lỗi rõ ràng');
  // Thêm 10 nhận xét: riêng khóa thứ hai có 11 dòng để kiểm tra trang cuối hết dòng.
  for (let i = 0; i < 10; i++) await review(second, await account(`extra-${i}`), 'Góp ý bổ sung ' + i);
  await page.goto(`${web}/instructor/reviews?replied=false&courseId=${second.id}&page=2`);
  await counts(page, 11, 12); assert.equal(await page.locator('article').count(), 1);
  await page.locator('article').getByRole('button', { name: 'Trả lời', exact: true }).click();
  await page.getByRole('textbox', { name: 'Nội dung phản hồi' }).fill('Cảm ơn bạn đã góp ý.');
  await page.getByRole('button', { name: 'Lưu phản hồi', exact: true }).click();
  await page.waitForURL(url => url.searchParams.get('page') === '1');
  await counts(page, 10, 11);
  assert.equal(new URL(page.url()).searchParams.get('courseId'), String(second.id));
  assert.equal(new URL(page.url()).searchParams.get('replied'), 'false');
  assert.equal(await page.locator('article').count(), 10);
  checks.push('Trả lời dòng cuối trang 2: về trang 1, giữ bộ lọc khóa/trạng thái, số đếm đúng');
  await page.setViewportSize({ width: 1366, height: 1000 });
  await page.screenshot({ path: path.join(output, 'inbox-desktop.png') });
  await page.setViewportSize({ width: 375, height: 1000 });
  await page.screenshot({ path: path.join(output, 'inbox-mobile.png') });
  assert.deepEqual(errors, []); checks.push('Không có lỗi JavaScript trong trình duyệt');
}
(async () => {
  try { await run(); }
  catch (e) { errors.push(e.message); console.error(e); process.exitCode = 1;
    for (let i = 0; i < pages.length; i++) await pages[i].screenshot({ path: path.join(output, `failure-${i}.png`) }).catch(() => {});
  } finally {
    if (browser) await browser.close();
    for (const course of fixtures) {
      for (const token of course.writers) {
        try { await api('DELETE', `/api/courses/${course.id}/reviews/me`, undefined, token); }
        catch (e) { errors.push('Cleanup: ' + e.message); process.exitCode = 1; }
      }
      try { await api('PATCH', `/api/courses/${course.id}/status`, { status: 'ARCHIVED' }, teacher); }
      catch (e) { errors.push('Archive: ' + e.message); process.exitCode = 1; }
    }
    fs.writeFileSync(path.join(output, 'inbox-ui-results.json'), JSON.stringify({ tag, courseIds: fixtures.map(c => c.id), checks, errors }, null, 2) + '\n');
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
