const { chromium, expect: baseExpect } = require(
  process.env.AUTH_PLAYWRIGHT_MODULE || "playwright/test",
);
const expect = baseExpect.configure({ timeout: 12000 });
const fs = require("fs");
const path = require("path");
const assert = require("node:assert/strict");
const out = process.env.AUTH_UI_OUTPUT || "target/auth-overview-browser";
fs.mkdirSync(out, { recursive: true });
const web = process.env.AUTH_WEB_URL || "http://localhost:3000";
const gateway = process.env.AUTH_GATEWAY_URL || "http://localhost:8080";
const password = "AdminUi@123456";
const results = [];
async function api(method, url, body, token, status = 200) {
  const response = await fetch(gateway + url, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  assert.equal(response.status, status, `${method} ${url}`);
  const json = await response.json();
  return json.data;
}
async function login(page, email, pass, next = "/admin/users") {
  await page.goto(web + "/login?next=" + encodeURIComponent(next));
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(pass);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.waitForURL((url) => url.pathname !== "/login");
}
async function search(page, keyword, role = "", status = "") {
  await page.locator("#user-keyword").fill(keyword);
  await page.locator("#user-role").selectOption(role);
  await page.locator("#user-status").selectOption(status);
  await page.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await page.waitForURL(
    (url) =>
      url.searchParams.get("keyword") === keyword &&
      url.searchParams.get("role") === role &&
      url.searchParams.get("status") === status &&
      !url.searchParams.has("page"),
  );
}
const rows = (page) =>
  page.viewportSize().width < 640
    ? page.getByRole("list", { name: "Danh sách người dùng" }).locator(":scope > li")
    : page.locator("tbody tr");
const rowFor = (page, email) => rows(page).filter({ hasText: email });
async function confirm(page) {
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: "Xác nhận", exact: true })
    .click();
}
async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
}

(async () => {
  const admin = await api("POST", "/api/auth/login", {
    email: "admin@elearning.hunre.edu.vn",
    password: "Admin@123456",
  });
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    for (const width of [1366, 768, 375]) {
      const prefix = `qa.admin-ui.${Date.now()}.${width}`;
      const people = [];
      for (let index = 0; index < 15; index++) {
        people.push(
          await api(
            "POST",
            "/api/auth/register",
            {
              email: `${prefix}.${String(index).padStart(2, "0")}@example.com`,
              fullName: `UI Người dùng ${String(index).padStart(2, "0")}`,
              password,
            },
            null,
            201,
          ),
        );
      }
      await api(
        "PATCH",
        `/api/users/${people[12].id}/roles`,
        { roles: ["ROLE_STUDENT", "ROLE_INSTRUCTOR"] },
        admin.accessToken,
      );
      await api(
        "PATCH",
        `/api/users/${people[13].id}/roles`,
        { roles: ["ROLE_STUDENT", "ROLE_ADMIN"] },
        admin.accessToken,
      );
      await api(
        "PATCH",
        `/api/users/${people[14].id}/status`,
        { status: "LOCKED" },
        admin.accessToken,
      );
      const oldSession = await api("POST", "/api/auth/login", { email: people[0].email, password });
      const context = await browser.newContext({ viewport: { width, height: 900 } });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      const pass = (name) => results.push({ width, name, status: "PASS" });
      await page.goto(web + "/admin/users");
      await page.waitForURL(
        (url) => url.pathname === "/login" && url.searchParams.get("next") === "/admin/users",
      );
      pass("Anonymous user redirected to login");
      await login(page, "admin@elearning.hunre.edu.vn", "Admin@123456");
      await expect(
        page.getByRole("heading", { name: "Người dùng & quyền", exact: true }),
      ).toBeVisible();

      await page.goto(web + "/design");
      await expect(page.getByRole("heading").first()).toBeVisible();
      await page.goto(web + "/admin");
      await expect(
        page.getByRole("heading", { name: "Tổng quan quản trị", exact: true }),
      ).toBeVisible();
      const stats = await api("GET", "/api/users/stats", undefined, admin.accessToken);
      for (const [label, count] of [
        ["Tổng người dùng", stats.total],
        ["Giảng viên", stats.byRole.ROLE_INSTRUCTOR],
        ["Quản trị viên", stats.byRole.ROLE_ADMIN],
        ["Bị khóa", stats.byStatus.LOCKED],
        ["Mới trong 7 ngày", stats.newLast7Days],
      ]) {
        const card = page
          .locator('[data-slot="card"]')
          .filter({ has: page.getByText(label, { exact: true }) });
        await expect(card.locator(".tabular-nums")).toHaveText(String(count));
      }
      const newest = await api(
        "GET",
        "/api/users?sort=createdAt,desc&size=5",
        undefined,
        admin.accessToken,
      );
      await expect(page.locator("tbody tr")).toHaveCount(5);
      for (let i = 0; i < 5; i++)
        await expect(page.locator("tbody tr").nth(i)).toContainText(newest.content[i].email);
      await noOverflow(page);
      await page.waitForLoadState("networkidle");
      await page.screenshot({ path: path.join(out, `overview-${width}.png`), fullPage: true });
      if (width >= 768)
        await expect(page.getByRole("link", { name: "Tổng quan", exact: true })).toHaveAttribute(
          "data-active",
          "true",
        );
      pass("Overview shows live API counts, five newest accounts and active sidebar");
      await page.locator('a[href="/admin/users?status=LOCKED"]').click();
      await expect(page.locator("#user-status")).toHaveValue("LOCKED");
      await expect(rowFor(page, people[14].email)).toBeVisible();
      await page.goto(web + "/admin");
      await page.locator('a[href="/admin/users?role=ROLE_INSTRUCTOR"]').click();
      await expect(page.locator("#user-role")).toHaveValue("ROLE_INSTRUCTOR");
      await expect(rowFor(page, people[12].email)).toBeVisible();
      await page.goto(web + "/admin");
      await page.getByRole("link", { name: "Xem tất cả", exact: true }).click();
      await expect(page.locator("#user-keyword")).toBeVisible();
      pass("Locked/instructor cards and View all open the correct user list");
      await search(page, "qa.student");
      await expect(rows(page)).toHaveCount(1);
      await expect(rowFor(page, "qa.student@example.com")).toBeVisible();
      pass("Admin searches qa.student and sees exactly one account");
      await search(page, prefix, "ROLE_STUDENT", "ACTIVE");
      await expect(rows(page)).toHaveCount(12);
      await expect(page.getByText("14 người dùng · Trang 1/2", { exact: true })).toBeVisible();
      await page.waitForLoadState("networkidle");
      await noOverflow(page);
      await page.screenshot({ path: path.join(out, `users-${width}.png`), fullPage: width >= 640 });
      await page
        .getByRole("navigation", { name: "Phân trang" })
        .getByRole("link", { name: "Sau", exact: false })
        .click();
      await page.waitForURL((url) => url.searchParams.get("page") === "1");
      await expect(rows(page)).toHaveCount(2);
      assert.equal(new URL(page.url()).searchParams.get("keyword"), prefix);
      assert.equal(new URL(page.url()).searchParams.get("status"), "ACTIVE");
      pass("Pagination preserves keyword, role and status");
      await search(page, prefix, "ROLE_INSTRUCTOR", "ACTIVE");
      await expect(rows(page)).toHaveCount(1);
      await expect(rowFor(page, people[12].email)).toBeVisible();
      pass("Combined filters reset page and do not duplicate multi-role users");
      await search(page, prefix + "missing");
      await expect(page.getByRole("heading", { name: "Không tìm thấy người dùng" })).toBeVisible();
      pass("No matches show empty state");
      await search(page, people[0].email);
      await rowFor(page, people[0].email).getByRole("button", { name: "Cấp quyền" }).click();
      const dialog = page.getByRole("dialog", { name: "Cấp quyền người dùng", exact: true });
      await expect(dialog.getByRole("checkbox", { name: /Học viên/ })).toBeChecked();
      await expect(dialog.locator("#userId")).toHaveCount(0);
      await dialog.getByRole("checkbox", { name: /Giảng viên/ }).check();
      await dialog.getByRole("button", { name: "Cập nhật vai trò" }).click();
      await page.getByRole("alertdialog").getByRole("button", { name: "Hủy", exact: true }).click();
      assert.deepEqual(
        (await api("GET", "/api/users?keyword=" + people[0].email, undefined, admin.accessToken))
          .content[0].roles,
        ["ROLE_STUDENT"],
      );
      await dialog.getByRole("button", { name: "Cập nhật vai trò" }).click();
      await confirm(page);
      await expect(dialog).toHaveCount(0);
      await expect(
        rowFor(page, people[0].email).getByText("Giảng viên", { exact: true }),
      ).toBeVisible();
      await page.reload();
      await expect(
        rowFor(page, people[0].email).getByText("Giảng viên", { exact: true }),
      ).toBeVisible();
      pass("Role form targets selected row, preselects roles; cancel/save/reload work");
      await search(page, people[0].email, "", "ACTIVE");
      await rowFor(page, people[0].email)
        .getByRole("button", { name: "Khóa", exact: true })
        .click();
      await page.getByRole("alertdialog").getByRole("button", { name: "Hủy", exact: true }).click();
      await api("POST", "/api/auth/login", { email: people[0].email, password });
      const actionBox = await rowFor(page, people[0].email)
        .getByRole("button", { name: "Khóa", exact: true })
        .boundingBox();
      assert(
        actionBox.x >= 0 && actionBox.x + actionBox.width <= width,
        "Lock visible without horizontal scroll",
      );
      pass("Lock and role actions fit inside the viewport without horizontal scrolling");
      pass("Cancel locking leaves account active");
      await rowFor(page, people[0].email)
        .getByRole("button", { name: "Khóa", exact: true })
        .click();
      await page.evaluate(() => {
        window.closedDialogSamples = [];
        new MutationObserver(() => {
          const dialog = document.querySelector('[role="alertdialog"][data-state="closed"]');
          if (dialog) window.closedDialogSamples.push(dialog.textContent);
        }).observe(document.body, {
          subtree: true,
          childList: true,
          attributes: true,
          characterData: true,
        });
      });
      await confirm(page);
      await expect(page.getByRole("alertdialog")).toHaveCount(0);
      await expect(page.getByRole("heading", { name: "Không tìm thấy người dùng" })).toBeVisible();
      await api("POST", "/api/auth/login", { email: people[0].email, password }, null, 403);
      await api(
        "POST",
        "/api/auth/refresh-token",
        { refreshToken: oldSession.refreshToken },
        null,
        401,
      );
      await api("GET", "/api/auth/me", undefined, oldSession.accessToken);
      const closing = await page.evaluate(() => window.closedDialogSamples);
      assert(closing.length > 0, "Captured closing animation");
      assert(
        closing.every(
          (text) => text.includes(people[0].fullName) && text.includes(people[0].email),
        ),
        "Dialog keeps account during exit",
      );
      pass("Closing lock dialog retains name and email throughout exit animation");
      pass("Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works");
      await search(page, people[0].email, "", "LOCKED");
      await expect(
        rowFor(page, people[0].email).getByText("Đã khóa", { exact: true }),
      ).toBeVisible();
      await page.waitForLoadState("networkidle");
      await page.screenshot({ path: path.join(out, `locked-${width}.png`), fullPage: true });
      await rowFor(page, people[0].email)
        .getByRole("button", { name: "Mở khóa", exact: true })
        .click();
      await confirm(page);
      await expect(page.getByRole("alertdialog")).toHaveCount(0);
      await expect(page.getByRole("heading", { name: "Không tìm thấy người dùng" })).toBeVisible();
      await api("POST", "/api/auth/login", { email: people[0].email, password });
      await api(
        "POST",
        "/api/auth/refresh-token",
        { refreshToken: oldSession.refreshToken },
        null,
        401,
      );
      pass("Unlock updates filtered list; login succeeds without restoring old refresh");
      for (const email of ["admin@elearning.hunre.edu.vn", people[13].email]) {
        await search(page, email);
        await expect(
          rowFor(page, email).getByRole("button", { name: "Khóa", exact: true }),
        ).toBeDisabled();
      }
      pass("Self and other admin cannot be locked from UI");
      await search(page, people[1].email);
      await rowFor(page, people[1].email)
        .getByRole("button", { name: "Khóa", exact: true })
        .click();
      await api(
        "PATCH",
        `/api/users/${people[1].id}/roles`,
        { roles: ["ROLE_ADMIN", "ROLE_STUDENT"] },
        admin.accessToken,
      );
      await confirm(page);
      await expect(
        page
          .getByRole("alertdialog")
          .getByText("Không thể khóa chính mình hoặc tài khoản quản trị viên", { exact: true }),
      ).toBeVisible();
      await noOverflow(page);
      await page.screenshot({ path: path.join(out, `lock-error-${width}.png`), fullPage: true });
      await page.getByRole("alertdialog").getByRole("button", { name: "Hủy", exact: true }).click();
      pass("Account promoted after dialog opens: server rejects lock and error remains visible");
      await page.goto(web + "/admin/users?sort=passwordHash");
      await expect(page.getByText("Không tải được người dùng", { exact: true })).toBeVisible();
      await page.getByRole("link", { name: "Tải lại danh sách" }).click();
      await expect(rows(page).first()).toBeVisible();
      pass("API load error is visible and retry recovers");
      await noOverflow(page);
      assert.deepEqual(errors, []);
      pass("No page JavaScript errors or horizontal document overflow");
      await context.close();
      for (const person of [people[2], people[12]]) {
        const outsider = await browser.newContext({ viewport: { width, height: 900 } });
        const outsiderPage = await outsider.newPage();
        await login(outsiderPage, person.email, password);
        await outsiderPage.goto(web + "/admin/users");
        await outsiderPage.waitForURL("**/khong-co-quyen");
        await outsider.close();
      }
      pass("Student and instructor cannot open admin page");
    }
    fs.writeFileSync(
      path.join(out, "overview-ui-results.json"),
      JSON.stringify({ browser: "Microsoft Edge", mode: "production", results }, null, 2),
    );
    console.log(`Overview browser checks passed: ${results.length}`);
  } finally {
    await browser.close();
  }
})().catch((error) => {
  fs.writeFileSync(path.join(out, "overview-ui-partial.json"), JSON.stringify(results, null, 2));
  console.error(error);
  process.exitCode = 1;
});
