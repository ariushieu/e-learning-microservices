// Real local API + production web acceptance. Only use a disposable test database.
const { chromium, expect: baseExpect } = require(process.env.AUTH_PLAYWRIGHT_MODULE || "playwright/test");
const expect = baseExpect.configure({ timeout: 12000 });
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const web = process.env.AUTH_WEB_URL || "http://localhost:3000";
const gateway = process.env.AUTH_GATEWAY_URL || "http://localhost:8080";
const out = process.env.AUTH_UI_OUTPUT || "target/auth-sessions-browser";
fs.mkdirSync(out, { recursive: true });
const results = [];
const password = "SessionUi@123456";
async function api(endpoint, body, expected = 200, agent = "PostmanRuntime/7.43") {
  const res = await fetch(gateway + endpoint, {
    method: "POST", headers: { "Content-Type": "application/json", "User-Agent": agent },
    body: JSON.stringify(body),
  });
  assert.equal(res.status, expected, endpoint);
  return (await res.json()).data;
}
async function sessions(page) {
  return page.evaluate(async () => {
    const response = await fetch("/api/auth/sessions", { cache: "no-store" });
    if (!response.ok) throw new Error("Session list status " + response.status);
    return (await response.json()).data;
  });
}
async function discardAccess(context) {
  // Exercise the real refresh paths without forging a JWT or mocking auth APIs.
  const cookie = (await context.cookies()).find((c) => c.name === "el_access");
  assert.ok(cookie);
  await context.addCookies([{ ...cookie, expires: 1 }]);
}

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    for (const width of [1366, 768, 375]) {
      const context = await browser.newContext({ viewport: { width, height: 1000 } });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      const pass = (test) => results.push({ width, test, result: "PASS" });
      const email = `qa.sessions-ui.${Date.now()}.${width}@example.com`;
      await api("/api/auth/register", { email: ` ${email} `, password, fullName: "QA Session Browser" }, 201);
      const postman = await api("/api/auth/login", { email, password });
      if (width === 1366) await page.goto(web + "/design");
      await page.goto(web + "/login?next=/profile");
      await page.locator("#email").fill(email);
      await page.locator("#password").fill(password);
      await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
      await page.waitForURL("**/profile");
      const region = page.getByLabel("Quản lý phiên đăng nhập", { exact: true });
      const rows = region.getByRole("list", { name: "Danh sách phiên đăng nhập" }).locator("li");
      const dialog = page.getByRole("alertdialog");
      await expect(rows).toHaveCount(2);
      await expect(rows.filter({ hasText: "Thiết bị này" })).toContainText("Edge trên Windows");
      const initial = await sessions(page);
      let currentId = initial.find((s) => s.current).id;
      const postmanId = initial.find((s) => !s.current).id;
      pass("Browser User-Agent and current label; two distinct sessions");
      await region.scrollIntoViewIfNeeded();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
      await page.screenshot({ path: path.join(out, `sessions-${width}.png`), fullPage: true });
      pass("Responsive session actions without horizontal overflow");

      await region.getByRole("button", { name: "Đăng xuất Thiết bị khác", exact: true }).click();
      await expect(dialog).toBeVisible();
      await dialog.getByRole("button", { name: "Hủy", exact: true }).click();
      assert.equal((await sessions(page)).length, 2);
      pass("Cancel does not revoke a session");

      // Only failure presentation is injected; subsequent revocation hits the real backend.
      const endpoint = web + "/api/auth/sessions/" + postmanId;
      await page.route(endpoint, (route) => route.fulfill({ status: 503, contentType: "application/json",
        body: JSON.stringify({ success: false, message: "Không thể đăng xuất lúc này" }) }));
      await region.getByRole("button", { name: "Đăng xuất Thiết bị khác", exact: true }).click();
      await dialog.getByRole("button", { name: "Xác nhận", exact: true }).click();
      await expect(dialog.getByRole("alert")).toBeVisible();
      await expect(dialog).toBeVisible();
      await expect(dialog.getByRole("button", { name: "Xác nhận", exact: true })).toBeEnabled();
      pass("Failed revoke stays in dialog with retry enabled");
      await page.unroute(endpoint);
      await dialog.getByRole("button", { name: "Xác nhận", exact: true }).click();
      await expect(dialog).toBeHidden();
      await expect(rows).toHaveCount(1);
      await api("/api/auth/refresh-token", { refreshToken: postman.refreshToken }, 401);
      pass("Remote revoke blocks the real Postman refresh token");

      const first = await api("/api/auth/login", { email, password });
      const second = await api("/api/auth/login", { email, password });
      await region.getByRole("button", { name: "Tải lại phiên" }).click();
      await expect(rows).toHaveCount(3);
      await region.getByRole("button", { name: "Đăng xuất mọi thiết bị khác", exact: true }).click();
      await expect(dialog).toContainText("Bạn vẫn đăng nhập trên thiết bị này");
      await dialog.getByRole("button", { name: "Xác nhận", exact: true }).click();
      await expect(dialog).toBeHidden();
      await expect(rows).toHaveCount(1);
      await api("/api/auth/refresh-token", { refreshToken: first.refreshToken }, 401);
      await api("/api/auth/refresh-token", { refreshToken: second.refreshToken }, 401);
      assert.equal((await sessions(page))[0].id, currentId);
      pass("Revoke others keeps this session and revokes both remote tokens");

      // Profile's server action refreshes tokens after updating the name.
      await page.locator("#profile-full-name").fill("QA Updated Session");
      await page.getByRole("button", { name: "Lưu thông tin", exact: true }).click();
      await expect.poll(async () => (await sessions(page))[0].id).not.toBe(currentId);
      await expect(rows).toHaveCount(1);
      await expect(rows).toContainText("Edge trên Windows");
      await expect(rows).toContainText("Thiết bị này");
      currentId = (await sessions(page))[0].id;
      pass("Profile server-action rotation preserves browser identity and current label");

      await discardAccess(context);
      await page.goto(web + "/profile"); // proxy.ts refresh path
      await expect(rows).toContainText("Edge trên Windows");
      const afterProxy = (await sessions(page))[0];
      assert.notEqual(afterProxy.id, currentId);
      assert.equal(afterProxy.current, true);
      currentId = afterProxy.id;
      pass("Proxy refresh forwards browser User-Agent");

      await discardAccess(context);
      const afterBff = (await sessions(page))[0]; // route-handler refresh path
      assert.notEqual(afterBff.id, currentId);
      assert.equal(afterBff.device, "Edge trên Windows");
      assert.equal(afterBff.current, true);
      currentId = afterBff.id;
      pass("BFF refresh forwards browser User-Agent");

      const cookie = (await context.cookies()).find((c) => c.name === "el_access");
      // A structurally current but invalid signature exercises the 401 retry branch.
      const segments = cookie.value.split(".");
      segments[2] = (segments[2][0] === "A" ? "B" : "A") + segments[2].slice(1);
      await context.addCookies([{ ...cookie, value: segments.join(".") }]);
      const afterRetry = (await sessions(page))[0];
      assert.notEqual(afterRetry.id, currentId);
      assert.equal(afterRetry.device, "Edge trên Windows");
      assert.equal(afterRetry.current, true);
      pass("BFF 401 retry preserves browser identity");

      await page.goto(web + "/profile");
      await region.getByRole("button", { name: "Đăng xuất thiết bị này", exact: true }).click();
      await expect(dialog).toContainText("Bạn sẽ được đưa về trang đăng nhập");
      await dialog.getByRole("button", { name: "Xác nhận", exact: true }).click();
      await page.waitForURL("**/login");
      assert.equal((await context.cookies()).some((c) => ["el_access", "el_refresh"].includes(c.name)), false);
      await page.goto(web + "/profile");
      await page.waitForURL((url) => url.pathname === "/login" && url.searchParams.get("next") === "/profile");
      assert.deepEqual(errors, []);
      pass("Self revoke clears cookies, redirects, protects profile; no JS errors");
      await context.close();
    }
  } finally {
    await browser.close();
    fs.writeFileSync(path.join(out, "browser-results.json"), JSON.stringify(results, null, 2));
  }
  console.log(`${results.length} session browser checks passed`);
})().catch((error) => { console.error(error); process.exitCode = 1; });
