// Run against a local test stack; creates one QA account per viewport.
const { chromium, expect } = require(process.env.AUTH_PLAYWRIGHT_MODULE || "playwright/test");
const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");
const web = process.env.AUTH_WEB_URL || "http://localhost:3000";
const out = process.env.AUTH_UI_OUTPUT || "target/auth-email-browser";
fs.mkdirSync(out, { recursive: true });
const results = [];

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    for (const width of [1366, 768, 375]) {
      const context = await browser.newContext({ viewport: { width, height: 900 } });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await page.goto(web + "/register?next=/profile");
      const email = `qa.email-ui.${Date.now()}.${width}@example.com`;
      const submit = async (address, phone = "", password = "Student@123456") => {
        await page.locator("#fullName").fill("QA Email Browser");
        await page.locator("#email").fill(address);
        await page.locator("#phone").fill(phone);
        await page.locator("#password").fill(password);
        await page.locator("#confirmPassword").fill(password);
        const response = page.waitForResponse((r) => r.request().method() === "POST" && new URL(r.url()).pathname === "/register");
        await page.getByRole("button", { name: "Tạo tài khoản", exact: true }).click();
        await response;
      };
      const field = (id) => page.locator("#" + id).locator("..");
      for (const address of ["thử@example.com", "test@vídụ.com", "te\u0301st@example.com"]) {
        await submit(address);
        await expect(field("email")).toContainText("Email chỉ được chứa ký tự ASCII, không dùng chữ có dấu");
        await expect(page.locator("#email")).toHaveAttribute("aria-invalid", "true");
        await expect(page.locator("#email")).toHaveValue(address);
        assert.equal(new URL(page.url()).pathname, "/register");
        results.push({ width, case: "Inline ASCII error: " + address, result: "PASS" });
      }
      await page.screenshot({ path: path.join(out, `register-email-${width}.png`), fullPage: true });
      for (const [address, message] of [["", "Email không được để trống"], ["invalid-email", "Email không đúng định dạng"]]) {
        await submit(address);
        await expect(field("email")).toContainText(message);
        results.push({ width, case: "Email validation: " + (address || "empty"), result: "PASS" });
      }
      await submit(email, "abc");
      await expect(field("phone")).toContainText("Số điện thoại phải có 9–15 chữ số");
      results.push({ width, case: "Phone validation remains inline", result: "PASS" });
      await submit(email, "", "123");
      await expect(field("password")).toContainText("Mật khẩu phải từ 6 đến 50 ký tự");
      results.push({ width, case: "Password minimum enforced by server", result: "PASS" });
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
      await submit(email.toUpperCase());
      await page.waitForURL("**/profile");
      await expect(page.locator("main")).toContainText(email);
      assert.deepEqual(errors, []);
      results.push({ width, case: "Corrected ASCII registration logs in; no overflow or JS errors", result: "PASS" });
      await context.close();
    }
  } finally {
    await browser.close();
    fs.writeFileSync(path.join(out, "browser-results.json"), JSON.stringify(results, null, 2));
  }
  console.log(`${results.length} email browser checks passed`);
})().catch((error) => { console.error(error); process.exitCode = 1; });
