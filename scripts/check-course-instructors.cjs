// API qua gateway và trình duyệt production, fixture riêng; không xuất token.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web = process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output = process.env.COURSE_UI_OUTPUT || path.join(require('node:os').tmpdir(), 'course-instructors-check');
const password = process.env.COURSE_QA_PASSWORD || 'Test@123456';
const tag = 'profile-ui-' + Date.now(), checks = [], errors = [], fixtures = [];
let browser, admin, teacher, other, teacherId, otherId, page;
fs.mkdirSync(output, { recursive: true });
async function api(method, url, body, token, expected = 200) {
  const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json',
    ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  assert.equal(r.status, expected, method + ' ' + url);
  return (await r.json()).data;
}
async function login(email, pass = password) {
  return api('POST', '/api/auth/login', { email, password: pass });
}
async function account(key, name, instructor = false) {
  const email = `${tag}-${key}@example.com`;
  await api('POST', '/api/auth/register', { email, password, fullName: name }, null, 201);
  const auth = await login(email);
  if (!instructor) return auth;
  await api('PATCH', `/api/users/${auth.user.id}/roles`, { roles: ['ROLE_STUDENT', 'ROLE_INSTRUCTOR'] }, admin);
  return login(email);
}
async function retry(fn) {
  let error;
  for (let i = 0; i < 30; i++) {
    try { return await fn(); } catch (e) { error = e; await new Promise(r => setTimeout(r, 500)); }
  }
  throw error;
}
async function makeCourse(title, categoryId, token = teacher, published = true) {
  const c = await api('POST', '/api/courses', { title, slug: tag + '-' + fixtures.length,
    categoryId, level: 'BEGINNER', price: 0, language: 'vi' }, token, 201);
  const fixture = { id: c.id, token, title, writers: [] }; fixtures.push(fixture);
  if (published) await api('PATCH', `/api/courses/${c.id}/status`, { status: 'PUBLISHED' }, token);
  return fixture;
}
async function review(course, token, rating) {
  await retry(() => api('POST', '/api/enrollments', { courseId: course.id }, token, 201));
  await retry(async () => assert.equal((await api('GET', `/api/courses/${course.id}/reviews/me`, undefined, token)).canReview, true));
  await api('PUT', `/api/courses/${course.id}/reviews/me`, { rating, comment: 'Góp ý cho hồ sơ giảng viên' }, token);
  course.writers.push(token);
}
async function open(token, url) {
  const context = await browser.newContext({ viewport: { width: 1366, height: 1000 } });
  if (token) await context.addCookies([{ name: 'el_access', value: token, url: web }]);
  const tab = await context.newPage(); tab.on('pageerror', e => errors.push(e.message));
  await tab.goto(web + url);
  return tab;
}
async function profileStats(tab, courses, students, rating, count) {
  await tab.getByTestId('instructor-course-count').getByText(String(courses), { exact: true }).waitFor();
  await tab.getByTestId('instructor-student-count').getByText(String(students), { exact: true }).waitFor();
  await tab.getByTestId('instructor-rating').getByText(rating, { exact: true }).waitFor();
  await tab.getByTestId('instructor-rating').getByText(`${count} lượt đánh giá`, { exact: true }).waitFor();
}
async function run() {
  admin = (await login(process.env.COURSE_ADMIN_EMAIL || 'admin@elearning.hunre.edu.vn', process.env.COURSE_ADMIN_PASSWORD || 'Admin@123456')).accessToken;
  const a = await account('A', 'Nguyễn Minh An', true), b = await account('B', 'Lê Thu Hà', true);
  teacher = a.accessToken; teacherId = a.user.id; other = b.accessToken; otherId = b.user.id;
  const s = (await account('S', 'Học viên S')).accessToken, t = (await account('T', 'Học viên T')).accessToken;
  const category = await api('POST', '/api/categories', { name: 'Kiến thức nền tảng ' + tag, slug: tag }, teacher, 201);
  const first = await makeCourse('Thiết kế hệ thống bền vững ' + tag, category.id);
  const second = await makeCourse('Kiến trúc phần mềm hiện đại ' + tag, category.id);
  await review(first, s, 5); await review(first, t, 5); await review(second, s, 2);
  const draft = await makeCourse('Bản nháp không công khai ' + tag, category.id, teacher, false);
  const archived = await makeCourse('Khóa đã lưu trữ ' + tag, category.id);
  await review(archived, s, 1);
  await api('PATCH', `/api/courses/${archived.id}/status`, { status: 'ARCHIVED' }, teacher);
  const draftB = await makeCourse('Bản nháp B ' + tag, category.id, other, false);
  const result = await api('GET', `/api/instructors/${teacherId}`);
  assert.deepEqual(result, { name: 'Nguyễn Minh An', publishedCourses: 2, totalStudents: 3, ratingAvg: 4, ratingCount: 3 });
  checks.push('MySQL/gateway: 2 khóa PUBLISHED, tổng 3 học viên, 3 đánh giá, điểm có trọng số 4.00');
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  page = await open(null, `/instructors/${teacherId}`);
  await page.getByRole('heading', { level: 1, name: /Nguyễn Minh An/ }).waitFor();
  await profileStats(page, 2, 3, '4/5', 3);
  assert.equal(new URL(page.url()).pathname, `/instructors/${teacherId}`);
  assert.equal(await page.locator('article').count(), 2);
  assert.equal(await page.locator(`a[href="/courses/${draft.id}"], a[href="/courses/${archived.id}"]`).count(), 0);
  checks.push('Khách không bị chuyển đăng nhập; số liệu đúng, không lộ DRAFT/ARCHIVED');
  for (const [role, token] of [['chủ khóa', teacher], ['admin', admin], ['học viên', s]]) {
    const tab = await open(token, `/instructors/${teacherId}`);
    await profileStats(tab, 2, 3, '4/5', 3);
    assert.equal(await tab.locator('article').count(), 2);
    assert.equal(await tab.locator(`a[href="/courses/${draft.id}"], a[href="/courses/${archived.id}"]`).count(), 0);
    checks.push(`${role} cũng chỉ thấy khóa công khai trên trang hồ sơ`);
    await tab.context().close();
  }
  await page.goto(`${web}/?keyword=${encodeURIComponent(tag)}`);
  const firstCard = page.locator('article').filter({ has: page.getByRole('link', { name: first.title, exact: true }) });
  await firstCard.waitFor();
  assert.equal(await page.locator('a a').count(), 0);
  await firstCard.getByRole('link', { name: 'Nguyễn Minh An', exact: true }).click();
  await page.waitForURL(`**/instructors/${teacherId}`); await profileStats(page, 2, 3, '4/5', 3);
  checks.push('Tên giảng viên trên thẻ trang chủ mở hồ sơ, HTML không lồng liên kết');
  await page.getByRole('link', { name: first.title, exact: true }).click();
  await page.waitForURL(`**/courses/${first.id}`);
  await page.getByRole('link', { name: 'Nguyễn Minh An', exact: true }).click();
  await page.waitForURL(`**/instructors/${teacherId}`);
  checks.push('Liên kết khóa học và tên giảng viên trên trang chi tiết đi đúng trang');
  await page.locator('article').first().click({ position: { x: 30, y: 30 } });
  await page.waitForURL(/\/courses\/\d+$/);
  checks.push('Bấm ảnh bìa thẻ vẫn mở khóa học sau khi tách liên kết giảng viên');
  await page.goto(`${web}/instructors/${teacherId}`); await profileStats(page, 2, 3, '4/5', 3);
  for (const width of [375, 768, 1366]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.evaluate(() => window.scrollTo(0, 0));
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1));
    await page.screenshot({ path: path.join(output, `instructor-${width}.png`), fullPage: true });
    checks.push(`Trang hồ sơ không tràn ngang ở ${width}px`);
  }
  for (const id of [otherId, '9007199254740991', 'abc']) {
    await page.goto(`${web}/instructors/${id}`);
    await page.getByRole('heading', { name: 'Không tìm thấy trang', exact: true }).waitFor();
    assert.ok(!page.url().includes('/login'));
  }
  checks.push('Chỉ có DRAFT, ID thiếu/sai: trang 404 cho khách, không chuyển đăng nhập');
  await api('PATCH', `/api/courses/${draftB.id}/status`, { status: 'PUBLISHED' }, other);
  await page.goto(`${web}/instructors/${otherId}`); await profileStats(page, 1, 0, 'Chưa có', 0);
  checks.push('Giảng viên mới có khóa PUBLISHED: 0 học viên, chưa có đánh giá');
  // Tạo 13 khóa công khai tổng cộng để kiểm phân trang 12 dòng.
  for (let i = 0; i < 11; i++) await makeCourse(`Bài giảng bổ sung ${i} ${tag}`, category.id);
  await page.goto(`${web}/instructors/${teacherId}`); await profileStats(page, 13, 3, '4/5', 3);
  assert.equal(await page.locator('article').count(), 12);
  const ids = () => page.locator('article h3 a').evaluateAll(links => links.map(l => l.getAttribute('href')));
  const page1 = await ids();
  await page.getByRole('navigation', { name: 'Phân trang' }).getByRole('link', { name: 'Sau', exact: true }).click();
  await page.waitForURL(url => url.searchParams.get('page') === '2');
  await retry(async () => assert.equal(await page.locator('article').count(), 1));
  const page2 = await ids(); assert.equal(page1.includes(page2[0]), false);
  await profileStats(page, 13, 3, '4/5', 3);
  checks.push('13 khóa: trang 1 có 12, trang 2 có 1 không trùng, số liệu tổng không đổi');
  await page.reload(); await profileStats(page, 13, 3, '4/5', 3);
  assert.deepEqual(await ids(), page2);
  await page.goto(`${web}/instructors/${teacherId}?page=999`);
  await page.waitForURL(url => url.searchParams.get('page') === '2');
  checks.push('Tải lại giữ phân trang; trang vượt tổng quay về trang hợp lệ cuối');
  await page.goto(`${web}/instructor/reviews`); await page.waitForURL('**/login?**');
  checks.push('Sửa proxy không mở công khai khu /instructor/reviews');
  assert.deepEqual(errors, []); checks.push('Không có lỗi JavaScript');
}
async function outage() {
  const id = process.env.COURSE_PROFILE_OUTAGE_ID;
  assert.match(id, /^\d+$/);
  await api('GET', `/api/instructors/${id}`, undefined, undefined, 502);
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  page = await open(null, `/instructors/${id}`);
  await page.getByRole('alert').filter({ hasText: 'Không tải được hồ sơ' }).waitFor();
  assert.equal(await page.getByRole('heading', { name: 'Không tìm thấy trang', exact: true }).count(), 0);
  assert.ok(!page.url().includes('/login'));
  checks.push('Course-service dừng: gateway 502, web hiện lỗi tải hồ sơ thay vì 404/đăng nhập');
  assert.deepEqual(errors, []); checks.push('Trang lỗi không có lỗi JavaScript');
}
(async () => {
  try { await (process.env.COURSE_PROFILE_OUTAGE_ID ? outage() : run()); }
  catch (e) { errors.push(e.message); console.error(e); process.exitCode = 1;
    if (page) await page.screenshot({ path: path.join(output, 'failure.png'), fullPage: true }).catch(() => {});
  } finally {
    if (browser) await browser.close();
    for (const course of fixtures) {
      try {
        for (const token of course.writers) await api('DELETE', `/api/courses/${course.id}/reviews/me`, undefined, token);
        await api('PATCH', `/api/courses/${course.id}/status`, { status: 'ARCHIVED' }, course.token);
      } catch (e) { errors.push('Cleanup: ' + e.message); process.exitCode = 1; }
    }
    const reportName = process.env.COURSE_PROFILE_OUTAGE_ID ? 'instructor-outage-results.json' : 'instructor-ui-results.json';
    fs.writeFileSync(path.join(output, reportName), JSON.stringify({ tag, teacherId, otherId,
      courseIds: fixtures.map(c => c.id), checks, errors }, null, 2) + '\n');
    console.log(JSON.stringify({ passed: checks.length, errors }));
  }
})();
