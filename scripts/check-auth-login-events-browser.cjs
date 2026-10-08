// Run only against a disposable QA database and a production frontend build.
const { chromium, expect: baseExpect } = require(process.env.AUTH_PLAYWRIGHT_MODULE || "playwright/test");
const expect = baseExpect.configure({ timeout: 12000 });
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const web = process.env.AUTH_WEB_URL || "http://localhost:3000";
const gateway = process.env.AUTH_GATEWAY_URL || "http://localhost:8080";
const out = process.env.AUTH_UI_OUTPUT || "target/auth-login-events-browser";
const password = "LoginActivity@123456";
const results = [];
fs.mkdirSync(out, { recursive: true });

async function api(endpoint, body, status = 200) {
  const response = await fetch(gateway + endpoint, {
    method: "POST", headers: { "Content-Type": "application/json", "User-Agent": "PostmanRuntime/7.43" },
    body: JSON.stringify(body),
  });
  assert.equal(response.status, status, endpoint);
  return (await response.json()).data;
}
async function login(page, email) {
  await page.goto(web + "/login?next=/profile");
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.waitForURL("**/profile");
}

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    for (const width of [1366, 768, 375]) {
      const context = await browser.newContext({ viewport: { width, height: 1000 } });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await context.route("**/api/notifications**", (route) => route.fulfill({
        status: 503, contentType: "application/json", body: JSON.stringify({ success: false, message: "Outside QA stack" }),
      }));
      const pass = (test) => {
        results.push({ width, test, result: "PASS" });
        fs.writeFileSync(path.join(out, "activity-browser-results.json"), JSON.stringify(results, null, 2));
      };
      const email = `qa.activity-ui.${Date.now()}.${width}@example.com`;
      await api("/api/auth/register", { email, password, fullName: "QA Login Activity" }, 201);
      for (let i = 0; i < 3; i++) await api("/api/auth/login", { email, password: "Incorrect@123456" }, 401);
      if (width === 1366) await page.goto(web + "/design");
      await login(page, email);
      const warning = page.getByText("Có 3 lần đăng nhập sai vào tài khoản của bạn", { exact: true });
      const activity = page.locator("section").filter({ has: page.getByRole("heading", { name: "Hoạt động đăng nhập", exact: true }) });
      await expect(warning).toBeVisible();
      await expect(activity.getByText("Sai mật khẩu", { exact: true })).toHaveCount(3);
      await expect(activity.getByText("Thành công", { exact: true })).toHaveCount(1);
      await expect(activity).toContainText("Edge trên Windows");
      pass("Three failed logins persist and show warning and four real history rows");
      await page.getByRole("link", { name: "Xem phiên đăng nhập", exact: true }).click();
      await expect(page.locator("#login-sessions")).toBeInViewport();
      await page.getByRole("link", { name: "Đổi mật khẩu", exact: true }).click();
      await expect(page.locator("#change-password")).toBeInViewport();
      pass("Warning links reach session management and password change");
      await page.reload();
      await expect(warning).toBeVisible();
      pass("Reload preserves warning without adding another successful login");
      await activity.scrollIntoViewIfNeeded();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
      await page.screenshot({ path: path.join(out, `activity-${width}.png`), fullPage: true });
      pass("History and warning fit viewport without horizontal overflow");

      // Inject only an error response. The retry uses the real gateway and MySQL.
      const endpoint = "**/api/auth/login-events?*";
      await page.route(endpoint, (route) => route.fulfill({ status: 503, contentType: "application/json",
        body: JSON.stringify({ success: false, message: "Không tải được hoạt động đăng nhập." }) }));
      await activity.getByRole("button", { name: "Tải lại hoạt động" }).click();
      await expect(activity.getByRole("alert")).toBeVisible();
      await expect(activity.getByText("Sai mật khẩu", { exact: true })).toHaveCount(3);
      await page.unroute(endpoint);
      await activity.getByRole("button", { name: "Thử lại" }).click();
      await expect(activity.getByRole("alert")).toHaveCount(0);
      await expect(activity.getByText("Sai mật khẩu", { exact: true })).toHaveCount(3);
      pass("History reload failure preserves rows and retry recovers real data");

      await context.clearCookies();
      await login(page, email);
      await expect(warning).toHaveCount(0);
      await expect(activity.getByText("Thành công", { exact: true })).toHaveCount(2);
      pass("Next successful browser login clears warning");
      for (let i = 0; i < 8; i++) await api("/api/auth/login", { email, password: "Incorrect@123456" }, 401);
      await activity.getByRole("button", { name: "Tải lại hoạt động" }).click();
      await expect(activity.locator("tbody tr")).toHaveCount(10);
      await expect(activity.getByText("Sai mật khẩu", { exact: true })).toHaveCount(8);
      pass("History limits display to ten newest rows");
      assert.deepEqual(errors, []);
      pass("No browser JavaScript errors");
      await context.close();
    }
  } finally {
    await browser.close();
  }
  console.log(JSON.stringify({ checks: results.length, passed: results.length }));
})().catch((error) => { console.error(error); process.exitCode = 1; });
