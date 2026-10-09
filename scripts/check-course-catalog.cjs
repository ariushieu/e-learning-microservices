// Kiểm tra trình duyệt với fixture riêng qua gateway; chạy sau collection chuẩn bị QA.
const { chromium } = require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const base = process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web = process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output = process.env.COURSE_UI_OUTPUT || path.join(os.tmpdir(), 'course-catalog-check');
fs.mkdirSync(output, { recursive: true });
const tag = 'catalog-ui-' + Date.now();
const courses = [], categories = [], checks = [], errors = [];
let token, browser;
async function api(method, url, body, expected = 200) {
  const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  assert.equal(r.status, expected, method + ' ' + url);
  return (await r.json()).data;
}
async function run() {
  token = (await api('POST', '/api/auth/login', { email: 'qa.instructor.a@example.com', password: process.env.COURSE_QA_PASSWORD || 'Test@123456' })).accessToken;
  for (let i = 0; i < 2; i++) {
    const c = await api('POST', '/api/categories', { name: `${tag} danh mục ${i}`, slug: `${tag}-${i}`, position: 0 }, 201);
    categories.push(c.id);
  }
  const child = await api('POST', '/api/categories', { name: `${tag} danh mục con`, slug: `${tag}-child`, parentId: categories[0], position: 0 }, 201);
  categories.push(child.id);
  for (let i = 0; i < 16; i++) {
    const c = await api('POST', '/api/courses', { title: `${tag} khóa ${String(i).padStart(2, '0')}`, slug: `${tag}-${i}`, categoryId: categories[[1,3].includes(i) ? 2 : i === 15 ? 1 : 0], level: i === 14 ? 'ADVANCED' : 'BEGINNER', price: (15-i)*100, language: 'vi' }, 201);
    courses.push(c.id);
    await api('PATCH', `/api/courses/${c.id}/status`, { status: 'PUBLISHED' });
  }
  browser = await chromium.launch({ headless: true, ...(process.env.COURSE_BROWSER_CHANNEL ? { channel: process.env.COURSE_BROWSER_CHANNEL } : {}) });
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  page.on('pageerror', e => errors.push(e.message));
  const cards = () => page.locator('section[aria-label="Danh sách khóa học"] a[href^="/courses/"]');
  const ids = async () => cards().evaluateAll(links => links.map(l => Number(l.getAttribute('href').split('/').pop())));
  const chips = () => page.getByRole('navigation', { name: 'Lọc nhanh theo danh mục' });
  const initial = `${web}/courses?keyword=${tag}&level=BEGINNER&sort=price,asc`;
  await page.goto(web + '/design');
  await page.goto(initial);
  await chips().getByRole('link', { name: `${tag} danh mục 0`, exact: true }).click();
  await page.waitForURL(u => u.searchParams.get('categoryId') === String(categories[0]));
  const query = new URL(page.url()).searchParams;
  assert.equal(query.get('keyword'), tag); assert.equal(query.get('level'), 'BEGINNER'); assert.equal(query.get('sort'), 'price,asc'); assert.equal(query.has('page'), false);
  assert.equal(await page.locator('input[type=hidden][name=categoryId]').inputValue(), String(categories[0]));
  checks.push('Chip giữ keyword/level/sort, đồng bộ ô chọn và về trang đầu');
  await cards().first().waitFor(); assert.deepEqual(await ids(), courses.slice(2,14).reverse());
  checks.push('Giá tăng lọc đúng danh mục/trình độ, tối đa 12 khóa');
  await page.getByRole('navigation', { name: 'Phân trang' }).getByRole('link', { name: 'Sau' }).click();
  await page.waitForURL(u => u.searchParams.get('page') === '2'); await cards().first().waitFor();
  assert.deepEqual(await ids(), courses.slice(0,2).reverse());
  assert.equal(new URL(page.url()).searchParams.get('categoryId'), String(categories[0]));
  assert.equal(new URL(page.url()).searchParams.get('sort'), 'price,asc');
  checks.push('Trang 2 giữ bộ lọc và thứ tự');
  for (const sort of ['price,desc','createdAt,desc','studentCount,desc']) {
    // Selects submit on change; there is no separate "Lọc" button any more.
    await page.getByRole('combobox', { name: 'Sắp xếp', exact: true }).selectOption(sort);
    await page.waitForURL(u => u.searchParams.get('sort') === sort && !u.searchParams.has('page'));
    await cards().first().waitFor();
    assert.deepEqual(await ids(), sort === 'price,desc' ? courses.slice(0,12) : courses.slice(2,14).reverse());
    checks.push(`Đổi sort ${sort} qua form, về trang đầu; khóa bằng nhau xếp theo id`);
  }
  await page.getByRole('searchbox', { name: 'Từ khóa', exact: true }).fill(tag + ' khóa 03');
  await page.getByRole('button', { name: 'Tìm kiếm', exact: true }).click();
  await page.waitForURL(u => u.searchParams.get('keyword') === tag + ' khóa 03'); await cards().first().waitFor();
  assert.deepEqual(await ids(), [courses[3]]);
  assert.equal(new URL(page.url()).searchParams.get('sort'), 'studentCount,desc');
  checks.push('Tìm kiếm giữ danh mục/trình độ/sort');
  await page.goto(initial + `&categoryId=${categories[0]}`);
  await page.getByRole('navigation', { name: `Danh mục con của ${tag} danh mục 0` }).getByRole('link', { name: `${tag} danh mục con`, exact: true }).click();
  await page.waitForURL(u => u.searchParams.get('categoryId') === String(child.id));
  await cards().first().waitFor();
  assert.deepEqual(await ids(), [courses[3], courses[1]]);
  checks.push('Chọn danh mục con chỉ trả khóa con, không lẫn khóa cha hoặc danh mục khác');
  assert.equal(await chips().getByRole('link', { name: `${tag} danh mục 0`, exact: true }).getAttribute('aria-current'), 'true');
  assert.equal(await chips().locator('[aria-current="true"]').count(), 1);
  checks.push('Chọn danh mục con tô đúng một chip cha');
  await page.reload(); await cards().first().waitFor();
  assert.equal(await chips().getByRole('link', { name: `${tag} danh mục 0`, exact: true }).getAttribute('aria-current'), 'true');
  assert.deepEqual(await ids(), [courses[3], courses[1]]);
  checks.push('URL danh mục con giữ kết quả và chip cha sau tải lại');
  for (const width of [375,768,1366]) {
    await page.setViewportSize({ width, height: 900 }); await page.goto(initial + `&categoryId=${categories[0]}`); await cards().first().waitFor();
    const bounds = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
    assert.ok(bounds.scroll <= bounds.client + 1, JSON.stringify(bounds)); checks.push(`Không tràn trang tại ${width}px`);
    const rail = await chips().evaluate(e => ({ scroll: e.scrollWidth, client: e.clientWidth, overflow: getComputedStyle(e).overflowX }));
    assert.ok(rail.scroll > rail.client); assert.equal(rail.overflow, 'auto'); checks.push(`Chip cuộn ngang tại ${width}px`);
    await page.screenshot({ path: path.join(output, `catalog-${width}.png`) });
  }
  await chips().getByRole('link', { name: 'Tất cả', exact: true }).click(); await page.waitForURL(u => !u.searchParams.has('categoryId'));
  assert.equal(await page.locator('input[type=hidden][name=categoryId]').count(), 0);
  checks.push('Chip Tất cả bỏ danh mục khỏi bộ lọc sau điều hướng client');
  await chips().getByRole('link', { name: 'Tất cả', exact: true }).focus(); await page.keyboard.press('Tab');
  assert.ok(await page.evaluate(() => !!document.activeElement.closest('nav[aria-label="Lọc nhanh theo danh mục"]')));
  checks.push('Tab chuyển tới chip tiếp theo');
  await page.goto(`${web}/courses?keyword=missing-${tag}`); await page.getByText('Không tìm thấy khóa học nào phù hợp', { exact: true }).waitFor();
  checks.push('Trạng thái không có kết quả');
  await page.goto(`${web}/courses?keyword=${tag}&sort=invalid`); await page.getByRole('combobox', { name: 'Sắp xếp', exact: true }).waitFor();
  assert.equal(await page.getByRole('combobox', { name: 'Sắp xếp', exact: true }).inputValue(), 'createdAt,desc');
  checks.push('URL sort lạ trở về Mới nhất');
  assert.deepEqual(errors, []); checks.push('Không có lỗi JavaScript');
}
(async () => {
  try { await run(); }
  catch (error) { errors.push(error.message); console.error(error); process.exitCode = 1; }
  finally {
    if (browser) await browser.close();
    // Chỉ dọn ID đã tạo trong lần chạy này; không đụng dữ liệu sẵn có hoặc fixture Postman.
    for (const id of courses) { try { await api('PATCH', `/api/courses/${id}/status`, { status: 'DRAFT' }); await api('DELETE', `/api/courses/${id}`); } catch (e) { errors.push(`Cleanup course ${id}: ${e.message}`); process.exitCode=1; } }
    for (const id of [...categories].reverse()) { try { await api('DELETE', `/api/categories/${id}`); } catch (e) { errors.push(`Cleanup category ${id}: ${e.message}`); process.exitCode=1; } }
    fs.writeFileSync(path.join(output, 'catalog-ui-results.json'), JSON.stringify({tag, checks, errors}, null, 2));
    console.log(JSON.stringify({passed:checks.length, errors}));
  }
})();
