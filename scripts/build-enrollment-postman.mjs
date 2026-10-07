/**
 * Sinh collection tự chứa; không xuất token hoặc ID thực tế vào Git.
 * Chạy từ gốc repo: node scripts/build-enrollment-postman.mjs
 */
import { writeFileSync } from "node:fs";

const folders = [];
let current;
const vars = { baseUrl: "http://localhost:8080", missingId: "9223372036854775807",
  adminToken: "", adminId: "", adminRefresh: "", tokenA: "", instructorAId: "", refreshA: "",
  tokenB: "", instructorBId: "", refreshB: "", studentToken: "", studentId: "", studentRefresh: "" };
const variable = (key) => "{{" + key + "}}";
const V = variable;
const json = (body) => JSON.stringify(body, null, 2).replace(/"\{\{(\w*Id)\}\}"/g, "{{$1}}");
const event = (listen, code) => ({ listen, script: { type: "text/javascript", exec: code.split("\n") } });
const folder = (name, description) => { current = { name, description, item: [] }; folders.push(current); };
const save = (key, path = "d.id") => "pm.collectionVariables.set(" + JSON.stringify(key) + ", " + path + ");";
const eq = (path, expected) => "pm.expect(" + path + ").to.eql(" + expected + ");";
const cv = (key) => "pm.collectionVariables.get(" + JSON.stringify(key) + ")";
const id = (key) => "Number(" + cv(key) + ")";
function request(name, method, path, status, body, token = "studentToken", check = "", options = {}) {
  const success = Array.isArray(status) ? status.some((s) => s < 400) : status < 400;
  const code = [
    "const cv = (key) => pm.collectionVariables.get(key);",
    "const id = (key) => Number(cv(key));",
    "let response; try { response = pm.response.json(); } catch { response = {}; }",
    "const d = response.data;",
    ...(options.poll ? [
      "const ready = (" + options.poll + ");",
      "const pollKey = 'poll:' + pm.info.requestName;",
      "const attempts = Number(cv(pollKey) || 0);",
      "if (!ready && pm.response.code < 500 && attempts < 15) {",
      "  pm.collectionVariables.set(pollKey, attempts + 1);",
      "  setTimeout(() => pm.execution.setNextRequest(pm.info.requestName), 2000);",
      "} else {",
      "  pm.collectionVariables.unset(pollKey);",
    ] : []),
    "pm.test(" + JSON.stringify(name + " — HTTP " + status) + ", () => {",
    Array.isArray(status) ? "pm.expect(pm.response.code).to.be.oneOf(" + JSON.stringify(status) + ");" : "pm.response.to.have.status(" + status + ");",
    "});",
    ...(success ? ["if (pm.response.code < 400) {",
      "pm.test(" + JSON.stringify(name + " — dữ liệu") + ", () => {",
      "pm.expect(response.success).to.eql(true);", check, "});", "}"] : []),
    ...(!success ? ["pm.test('Lỗi có hợp đồng API', () => { pm.expect(response.success).to.eql(false); pm.expect(response.code).to.be.a('string'); });"] : []),
    ...(options.poll ? ["}"] : []),
    ...(options.stopOnFailure ? ["if (pm.response.code >= 400 && pm.response.code !== 409" + (options.poll ? " && attempts >= 15" : "") + ") pm.execution.setNextRequest(null);"] : []),
  ].join("\n");
  const req = { name, request: { method,
    auth: token ? { type: "bearer", bearer: [{ key: "token", value: V(token), type: "string" }] } : { type: "noauth" },
    header: body === undefined ? [] : [{ key: "Content-Type", value: "application/json" }],
    url: V("baseUrl") + path,
    ...(body === undefined ? {} : { body: { mode: "raw", raw: json(body), options: { raw: { language: "json" } } } }),
    ...(options.description ? { description: options.description } : {}),
  }, event: [event("test", code)] };
  if (options.pre) req.event.unshift(event("prerequest", options.pre));
  current.item.push(req);
  return req;
}
const R = request;
const GET = (name, path, status = 200, token = "studentToken", check = "", options = {}) => R(name, "GET", path, status, undefined, token, check, options);
const enroll = (name, status, token = "studentToken", courseId = V("publishedCourseId"), check = "", options = {}) =>
  R(name, "POST", "/api/enrollments", status, { courseId }, token, check, options);
const progressBody = (extra = {}) => ({ courseId: V("publishedCourseId"), status: "COMPLETED", watchedSeconds: 60, ...extra });
const progress = (name, status = 200, extra = {}, lesson = V("lessonAId"), token = "studentToken", check = "") =>
  R(name, "PUT", "/api/lessons/" + lesson + "/progress", status, progressBody(extra), token, check);
const progressGet = (name, check = "", options = {}) => GET(name, "/api/progress?courseId=" + V("publishedCourseId"), 200, "studentToken", check, options);
const own = "/api/enrollments/" + V("enrollmentId");
const cert = own + "/certificate";
const cancel = (name, status, token = "studentToken", suffix = own, body = { status: "CANCELLED" }, check = "") =>
  R(name, "PATCH", suffix + "/status", status, body, token, check);
const allOwn = "pm.expect(d.content).to.be.an('array'); d.content.forEach(e => pm.expect(e.userId).to.eql(id('studentId')));";
const percent = (n) => eq("Number(d.progressPercent)", n);
const unchangedCertificate = eq("d.certificateCode", cv("certificateCode"));
const completedLesson = eq("d.status", "'COMPLETED'") + eq("d.completedAt", cv("lessonCompletedAt"));
const snapshotReady = { poll: "pm.response.code === 201", stopOnFailure: true,
  description: "Runner thử lại 404 tối đa 30 giây để chờ course.updated. Nếu vẫn 404: dừng lượt chạy, ghi BLOCKED do snapshot; không sửa SQL." };

folder("0. Chuẩn bị", "Tài khoản dev cố định theo gateway.md. Toàn bộ ID lấy từ API. Mỗi lần chạy tạo khóa mới có runId; chạy toàn collection theo thứ tự, một iteration. Có thể cần RATE_LIMIT_ENABLED=false trên gateway.");
R("SETUP-01 Đăng nhập admin và bắt đầu lượt chạy", "POST", "/api/auth/login", 200,
  { email: "admin@elearning.hunre.edu.vn", password: "Admin@123456" }, null,
  save("adminToken", "d.accessToken") + save("adminRefresh", "d.refreshToken") + save("adminId", "d.user.id"),
  { stopOnFailure: true, pre: "pm.collectionVariables.set('runId', Date.now() + '-' + Math.random().toString(36).slice(2,8));\nObject.keys(pm.collectionVariables.toObject()).filter(k => k.startsWith('poll:')).forEach(k => pm.collectionVariables.unset(k));" });
const users = [
  ["A", "qa.instructor.a@example.com", "Giang vien A", "instructorAId", "tokenA", "refreshA", ["ROLE_STUDENT", "ROLE_INSTRUCTOR"]],
  ["B", "qa.instructor.b@example.com", "Giang vien B", "instructorBId", "tokenB", "refreshB", ["ROLE_STUDENT", "ROLE_INSTRUCTOR"]],
  ["S", "qa.student@example.com", "Hoc vien S", "studentId", "studentToken", "studentRefresh", ["ROLE_STUDENT"]],
];
for (const [label, email, fullName, idKey, tokenKey, refreshKey, roles] of users) {
  R("SETUP-02." + label + " Đăng ký (409 = đã tồn tại, phải đăng nhập bước sau)", "POST", "/api/auth/register", [201, 409],
    { email, fullName, password: "Test@123456" }, null, "", { stopOnFailure: true });
  R("SETUP-03." + label + " Đăng nhập lấy ID", "POST", "/api/auth/login", 200, { email, password: "Test@123456" }, null,
    save(idKey, "d.user.id"), { stopOnFailure: true });
  R("SETUP-04." + label + " Cấp vai trò bằng API", "PATCH", "/api/users/" + V(idKey) + "/roles", 200,
    { roles }, "adminToken", "", { stopOnFailure: true });
  R("SETUP-05." + label + " Đăng nhập lại lấy quyền mới", "POST", "/api/auth/login", 200, { email, password: "Test@123456" }, null,
    save(tokenKey, "d.accessToken") + save(refreshKey, "d.refreshToken") + save(idKey, "d.user.id") +
    (label === "S" ? save("studentName", "d.user.fullName") : ""), { stopOnFailure: true });
}
GET("SETUP-06 Xác nhận missingId không phải khóa thật", "/api/courses/" + V("missingId"), 404, "tokenA");
R("SETUP-07 Tạo danh mục riêng", "POST", "/api/categories", 201,
  { name: "Enrollment QA " + V("runId"), slug: "enrollment-qa-" + V("runId") }, "adminToken", save("categoryId"), { stopOnFailure: true });
for (const [prefix, count, courseKey] of [["main", 2, "publishedCourseId"], ["aux", 1, "auxCourseId"], ["race", 1, "raceCourseId"], ["archive", 1, "archivedCourseId"]]) {
  R("SETUP-08." + prefix + " Tạo khóa", "POST", "/api/courses", 201,
    { categoryId: V("categoryId"), title: "Enrollment " + prefix + " " + V("runId"), slug: "enrollment-" + prefix + "-" + V("runId"), description: "Fixture Postman qua gateway", price: 0, level: "BEGINNER", language: "vi" },
    "tokenA", save(courseKey) + (prefix === "main" ? save("courseTitle", "d.title") : ""), { stopOnFailure: true });
  R("SETUP-09." + prefix + " Tạo chương", "POST", "/api/courses/" + V(courseKey) + "/sections", 201,
    { title: "Chương kiểm thử", position: 0 }, "tokenA", save(prefix + "SectionId"), { stopOnFailure: true });
  for (let i = 1; i <= count; i++) {
    const lessonKey = prefix === "main" ? (i === 1 ? "lessonAId" : "lesson2Id") : prefix + "LessonId";
    R("SETUP-10." + prefix + "." + i + " Tạo bài", "POST", "/api/sections/" + V(prefix + "SectionId") + "/lessons", 201,
      { title: "Bài " + i, type: "ARTICLE", content: "Nội dung bài kiểm thử", durationSeconds: 60, position: i, isPreview: false },
      "tokenA", save(lessonKey), { stopOnFailure: true });
  }
  R("SETUP-11." + prefix + " Xuất bản và gửi course.updated", "PATCH", "/api/courses/" + V(courseKey) + "/status", 200,
    { status: "PUBLISHED" }, "tokenA", eq("d.status", "'PUBLISHED'"), { stopOnFailure: true });
}

folder("1. Ghi danh và quyền đọc", "ENROLL-01, 02, 03, 08; chưa thay đổi tiến độ.");
enroll("ENROLL-01.1 Ghi danh khóa đã đồng bộ", 201, "studentToken", V("publishedCourseId"),
  save("enrollmentId") + eq("d.userId", id("studentId")) + eq("d.status", "'ACTIVE'") + percent(0), snapshotReady);
enroll("ENROLL-01.2 Không token", 401, null);
enroll("ENROLL-01.3 Giảng viên tự ghi danh", 201, "tokenB", V("publishedCourseId"), save("enrollmentBId") + eq("d.userId", id("instructorBId")));
enroll("ENROLL-01.4 Khóa không tồn tại", 404, "studentToken", V("missingId"));
enroll("ENROLL-01.5 ID sai kiểu", 400, "studentToken", "abc");
for (const value of [0, -1]) enroll("ENROLL-01.6 ID " + value, 400, "studentToken", value);
R("ENROLL-01.6 Thiếu ID", "POST", "/api/enrollments", 400, {});
enroll("ENROLL-01.7 Ghi danh trùng", 409);
R("ENROLL-01.9 Không nhận userId từ body", "POST", "/api/enrollments", 201,
  { courseId: V("auxCourseId"), userId: V("instructorBId") }, "studentToken",
  save("auxEnrollmentId") + eq("d.userId", id("studentId")), snapshotReady);
GET("ENROLL-01.11 Đúng một thông báo ghi danh", "/api/notifications?size=100", 200, "studentToken",
  "pm.expect(d.content.filter(n => n.type === 'ENROLLMENT_SUCCESS' && n.content.includes(cv('courseTitle')))).to.have.length(1);",
  { poll: "d && d.content && d.content.some(n => n.type === 'ENROLLMENT_SUCCESS' && n.content.includes(cv('courseTitle')))" });
GET("ENROLL-02.1 Danh sách và metadata", "/api/enrollments?page=0&size=2&sort=enrolledAt,desc", 200, "studentToken",
  allOwn + eq("d.page", 0) + eq("d.size", 2) + "pm.expect(d.totalElements).to.be.at.least(2); pm.expect(d.totalPages).to.be.at.least(1);");
GET("ENROLL-02.2 Không token", "/api/enrollments", 401, null);
GET("ENROLL-02.3 Danh sách của B", "/api/enrollments?size=100", 200, "tokenB",
  "d.content.forEach(e => pm.expect(e.userId).to.eql(id('instructorBId'))); pm.expect(d.content.map(e => e.id)).not.to.include(id('enrollmentId'));");
GET("ENROLL-02.4 Trang rỗng", "/api/enrollments?page=999999&size=2", 200, "studentToken", eq("d.content", "[]"));
GET("ENROLL-02.5 Sort bịa", "/api/enrollments?sort=abcxyz", 400);
GET("ENROLL-02.6 Sort thuộc tính con bịa", "/api/enrollments?sort=course.nonexistent", 400);
GET("ENROLL-02.7 Không đọc hộ qua query", "/api/enrollments?userId=" + V("studentId") + "&size=100", 200, "tokenB",
  "d.content.forEach(e => pm.expect(e.userId).to.eql(id('instructorBId')));");
GET("ENROLL-03.1 Đọc lượt của mình", own, 200, "studentToken", eq("d.userId", id("studentId")));
GET("ENROLL-03.2 Không token", own, 401, null);
GET("ENROLL-03.3 B đọc hộ", own, 403, "tokenB");
GET("ENROLL-03.4 Admin đọc hộ", own, 403, "adminToken");
GET("ENROLL-03.5 ID không tồn tại", "/api/enrollments/" + V("missingId"), 404);
GET("ENROLL-03.6 ID sai kiểu", "/api/enrollments/abc", 400);
progressGet("ENROLL-08.8 Chưa học bài nào", percent(0) + eq("d.completedLessonsCount", 0) + eq("d.lessons", "[]"));
GET("ENROLL-08.2 Không token", "/api/progress?courseId=" + V("publishedCourseId"), 401, null);
GET("ENROLL-08.3 Người chưa ghi danh", "/api/progress?courseId=" + V("publishedCourseId"), 404, "tokenA");
GET("ENROLL-08.4 Khóa không có lượt", "/api/progress?courseId=" + V("missingId"), 404);
GET("ENROLL-08.5 ID sai kiểu", "/api/progress?courseId=abc", 400);
GET("ENROLL-08.6 Thiếu courseId", "/api/progress", 400);
R("SETUP-12 Lưu trữ khóa riêng", "PATCH", "/api/courses/" + V("archivedCourseId") + "/status", 200, { status: "ARCHIVED" }, "tokenA", "", { pre: "setTimeout(() => {}, 3000);" });
enroll("ENROLL-01.10 Khóa đã lưu trữ", 404, "studentToken", V("archivedCourseId"), "", { pre: "setTimeout(() => {}, 5000);",
  description: "404 kiểm khóa đã lưu trữ tại API. Trạng thái snapshot cần đối chiếu consumer/log; riêng HTTP không chứng minh Kafka đã đồng bộ." });

folder("2. Hủy và kích hoạt lại", "Hủy trước khi hoàn thành, dùng lại cùng enrollmentId.");
cancel("ENROLL-04.2 Không token", 401, null);
cancel("ENROLL-04.3 B hủy hộ", 403, "tokenB");
cancel("ENROLL-04.4 ID không tồn tại", 404, "studentToken", "/api/enrollments/" + V("missingId"));
cancel("ENROLL-04.5 ID sai kiểu", 400, "studentToken", "/api/enrollments/abc");
cancel("ENROLL-04.8 Enum sai", 400, "studentToken", own, { status: "UNKNOWN" });
cancel("ENROLL-04.8 Thiếu status", 400, "studentToken", own, {});
cancel("ENROLL-04.9 Không tự cấp hoàn thành", 422, "studentToken", own, { status: "COMPLETED" });
cancel("ENROLL-04.1 Hủy lượt ACTIVE", 200, "studentToken", own, { status: "CANCELLED" }, eq("d.status", "'CANCELLED'"));
cancel("ENROLL-04.7 Hủy lần hai", 200, "studentToken", own, { status: "CANCELLED" }, eq("d.status", "'CANCELLED'"));
GET("ENROLL-02.8 Danh sách giữ lượt đã hủy", "/api/enrollments?size=100", 200, "studentToken",
  "pm.expect(d.content.find(e => e.id === id('enrollmentId')).status).to.eql('CANCELLED');");
GET("ENROLL-03.7 Đọc lượt đã hủy", own, 200, "studentToken", eq("d.status", "'CANCELLED'"));
progress("ENROLL-07.8 Không cập nhật lượt đã hủy", 422);
enroll("ENROLL-01.8 Kích hoạt lại dùng ID cũ", 201, "studentToken", V("publishedCourseId"), eq("d.id", id("enrollmentId")) + eq("d.status", "'ACTIVE'"));

folder("3. Tiến độ và chứng chỉ", "Các ca hoàn thành, không giảm tiến độ và phân quyền chứng chỉ; khóa chính có đúng hai bài.");
GET("ENROLL-06.6 Lượt ACTIVE chưa có chứng chỉ", "/api/enrollments/" + V("auxEnrollmentId") + "/certificate", 404);
progress("ENROLL-07.2 Không token", 401, {}, V("lessonAId"), null);
progress("ENROLL-07.3 Người chưa ghi danh", 404, {}, V("lessonAId"), "tokenA");
progress("ENROLL-07.4 Khóa không có lượt", 404, { courseId: V("missingId") });
progress("ENROLL-07.5 ID bài sai kiểu", 400, {}, "abc");
progress("ENROLL-07.6 Giây xem âm", 400, { watchedSeconds: -1 });
progress("ENROLL-07.7 Sai status", 400, { status: "INVALID" });
R("ENROLL-07.7 Thiếu status", "PUT", "/api/lessons/" + V("lessonAId") + "/progress", 400, { courseId: V("publishedCourseId"), watchedSeconds: 60 });
progress("ENROLL-07.13 Bài không tồn tại", 404, {}, V("missingId"));
progress("ENROLL-07.13 Bài thuộc khóa khác", 404, {}, V("auxLessonId"));
progressGet("ENROLL-07.13 Kiểm tra không ghi tiến độ", percent(0) + eq("d.lessons", "[]"));
progress("ENROLL-07.1 Hoàn thành bài đầu", 200, {}, V("lessonAId"), "studentToken",
  eq("d.status", "'COMPLETED'") + eq("d.watchedSeconds", 60));
// Lấy mốc đã đọc lại từ DB (timestamp(6)), tránh so nano giây trước flush với micro giây đã lưu.
progressGet("ENROLL-07.1 Tiến độ 50%", percent(50) + eq("d.completedLessonsCount", 1) +
  save("lessonCompletedAt", "d.lessons.find(l => l.lessonId === id('lessonAId')).completedAt"));
progress("ENROLL-07.9 Gửi hoàn thành lần hai", 200, {}, V("lessonAId"), "studentToken", completedLesson);
progressGet("ENROLL-07.9 Không đếm trùng", percent(50) + eq("d.completedLessonsCount", 1));
progress("ENROLL-07.11 Giây xem không giảm", 200, { watchedSeconds: 10 }, V("lessonAId"), "studentToken", eq("d.watchedSeconds", 60));
for (const seconds of [90, 10]) progress("ENROLL-07.14 IN_PROGRESS với " + seconds + " giây", 200, { status: "IN_PROGRESS", watchedSeconds: seconds },
  V("lessonAId"), "studentToken", completedLesson + eq("d.watchedSeconds", 90));
progressGet("ENROLL-07.14 Giữ 50%", percent(50) + eq("d.completedLessonsCount", 1));
progress("ENROLL-07.12 B giả userId S", 200, { userId: V("studentId"), status: "IN_PROGRESS", watchedSeconds: 7 }, V("lessonAId"), "tokenB",
  eq("d.status", "'IN_PROGRESS'") + eq("d.watchedSeconds", 7));
progressGet("ENROLL-08.1 Tiến độ chỉ của S", eq("d.enrollmentId", id("enrollmentId")) + eq("d.totalLessonsCount", 2) + percent(50) +
  "pm.expect(d.lessons).to.have.length(1); pm.expect(d.lessons[0].status).to.eql('COMPLETED'); pm.expect(d.lessons[0].watchedSeconds).to.eql(90);");
GET("ENROLL-08.7 Không đọc hộ qua query", "/api/progress?courseId=" + V("publishedCourseId") + "&userId=" + V("studentId"), 200, "tokenB",
  eq("d.enrollmentId", id("enrollmentBId")) + percent(0) + eq("d.lessons[0].watchedSeconds", 7));
progress("ENROLL-07.10 Hoàn thành bài cuối", 200, {}, V("lesson2Id"), "studentToken", eq("d.status", "'COMPLETED'"));
progressGet("ENROLL-07.10 Hoàn thành 100%", percent(100) + eq("d.status", "'COMPLETED'") + eq("d.completedLessonsCount", 2) + save("certificateCode", "d.certificateCode"));
GET("ENROLL-07.10 Lưu mốc hoàn thành khóa", own, 200, "studentToken", save("enrollmentCompletedAt", "d.completedAt"));
const certCheck = eq("d.userId", id("studentId")) + eq("d.courseId", id("publishedCourseId")) + unchangedCertificate;
GET("ENROLL-06.1 Chứng chỉ được cấp", cert, 200, "studentToken", certCheck + save("certificateId") + save("issuedAt", "d.issuedAt"));
GET("ENROLL-06.2 Không token", cert, 401, null);
GET("ENROLL-06.3 B đọc chứng chỉ của S", cert, 403, "tokenB");
GET("ENROLL-06.4 Lượt không tồn tại", "/api/enrollments/" + V("missingId") + "/certificate", 404);
GET("ENROLL-06.5 ID sai kiểu", "/api/enrollments/abc/certificate", 400);
GET("ENROLL-06.7 Đọc lại không cấp trùng", cert, 200, "studentToken", certCheck + eq("d.id", id("certificateId")));
GET("ENROLL-06.8 Admin không đọc hộ", cert, 403, "adminToken");
for (let n = 1; n <= 2; n++) progressGet("ENROLL-08.9 Đọc sau hoàn thành lần " + n, percent(100) + unchangedCertificate);
cancel("ENROLL-04.6 Không hủy khóa hoàn thành", 422);
GET("ENROLL-04.6 Giữ trạng thái sau khi từ chối hủy", own, 200, "studentToken", eq("d.status", "'COMPLETED'"));
progress("ENROLL-07.15 Không thu hồi hoàn thành", 200, { status: "IN_PROGRESS", watchedSeconds: 10 }, V("lessonAId"), "studentToken", completedLesson);
progressGet("ENROLL-07.15 Giữ 100% và mã", percent(100) + eq("d.status", "'COMPLETED'") + unchangedCertificate);
GET("ENROLL-07.15 Giữ completedAt", own, 200, "studentToken", eq("d.completedAt", cv("enrollmentCompletedAt")));
GET("ENROLL-07.15 Giữ chứng chỉ cũ", cert, 200, "studentToken", unchangedCertificate + eq("d.id", id("certificateId")) + eq("d.issuedAt", cv("issuedAt")));
const notifications = "d.content.filter(n => n.content.includes(cv('courseTitle')) && ['COURSE_COMPLETED','CERTIFICATE_ISSUED'].includes(n.type))";
GET("ENROLL-07.15 Thông báo hoàn thành và chứng chỉ", "/api/notifications?size=100", 200, "studentToken",
  "const found = " + notifications + "; pm.expect(found.filter(n => n.type === 'COURSE_COMPLETED')).to.have.length(1); pm.expect(found.filter(n => n.type === 'CERTIFICATE_ISSUED')).to.have.length(1);",
  { poll: "d && d.content && (" + notifications + ").length >= 2" });
GET("ENROLL-07.15 Không thêm thông báo sau khi chờ outbox", "/api/notifications?size=100", 200, "studentToken",
  "pm.expect((" + notifications + ").filter(n => n.type === 'COURSE_COMPLETED')).to.have.length(1); pm.expect((" + notifications + ").filter(n => n.type === 'CERTIFICATE_ISSUED')).to.have.length(1);",
  { pre: "setTimeout(() => {}, 5000);" });

folder("4. Xác minh công khai", "ENROLL-10: đường dẫn mới, chỉ công khai tên học viên, tên khóa, ngày cấp và mã. Không dùng token.");
const verify = "/api/certificates/verify/" + V("certificateCode");
const publicFields = "pm.expect(Object.keys(d).sort()).to.eql(['certificateCode','courseTitle','issuedAt','learnerName']);" +
  eq("d.learnerName", cv("studentName")) + eq("d.courseTitle", cv("courseTitle")) + unchangedCertificate + eq("d.issuedAt", cv("issuedAt")) +
  "pm.expect(pm.response.headers.get('Cache-Control')).to.include('no-store');";
GET("ENROLL-10.1 Xác minh không đăng nhập", verify, 200, null, publicFields);
GET("ENROLL-10.2 Người khác vẫn thấy cùng dữ liệu công khai", verify, 200, "tokenB", publicFields);
GET("ENROLL-10.3 Mã không tồn tại", "/api/certificates/verify/CERT-NOT-FOUND-" + V("runId"), 404, null);
GET("ENROLL-10.4 Mã sai định dạng", "/api/certificates/verify/invalid", 404, null);
GET("ENROLL-10.5 Không giả người học qua query", verify + "?learnerName=Forged&userId=" + V("instructorBId"), 200, null, publicFields);
R("ENROLL-10.6 Chỉ mở GET, POST vẫn cần đăng nhập", "POST", verify, 401, {}, null);
GET("ENROLL-10.7 Không mở API danh sách chứng chỉ", "/api/certificates", 401, null);
R("SETUP-15 Đổi tên khóa sau khi cấp chứng chỉ", "PUT", "/api/courses/" + V("publishedCourseId"), 200,
  { categoryId: V("categoryId"), title: "Đổi tên sau cấp chứng chỉ " + V("runId"), slug: "enrollment-main-" + V("runId"), price: 0, level: "BEGINNER", language: "vi" },
  "tokenA", save("renamedTitle", "d.title"));
progressGet("ENROLL-10.9 Đợi tên mới đồng bộ", eq("d.courseTitle", cv("renamedTitle")), { poll: "d && d.courseTitle === cv('renamedTitle')" });
GET("ENROLL-10.9 Xác minh giữ tên khóa tại lúc cấp", verify, 200, null, publicFields);
GET("ENROLL-10.9 Chứng chỉ của chủ cũng giữ tên cũ", cert, 200, "studentToken", eq("d.courseTitle", cv("courseTitle")) + unchangedCertificate);

folder("5. Đồng thời và thêm bài sau hoàn thành", "Hai PUT trong ENROLL-07.16 chạy song song bằng pm.sendRequest; không dùng hai lượt tuần tự giả làm concurrent.");
enroll("SETUP-13 Lượt riêng cho ca đồng thời", 201, "studentToken", V("raceCourseId"), save("raceEnrollmentId"), snapshotReady);
GET("ENROLL-07.16 Hai cập nhật đồng thời", "/api/progress?courseId=" + V("raceCourseId"), 200, "studentToken",
  eq("d.status", "'COMPLETED'") + percent(100) + eq("d.lessons.length", 1) + eq("d.lessons[0].status", "'COMPLETED'") +
  eq("d.lessons[0].watchedSeconds", 120) + "pm.expect(d.certificateCode).to.be.a('string').and.not.empty;" + save("raceCertificateCode", "d.certificateCode"),
  { pre: [
    "[['COMPLETED', 60], ['IN_PROGRESS', 120]].forEach(([status, watchedSeconds]) => {",
    " pm.sendRequest({ url: pm.collectionVariables.get('baseUrl') + '/api/lessons/' + pm.collectionVariables.get('raceLessonId') + '/progress',",
    " method: 'PUT', header: {Authorization: 'Bearer ' + pm.collectionVariables.get('studentToken'), 'Content-Type':'application/json'},",
    " body: {mode:'raw', raw: JSON.stringify({courseId:Number(pm.collectionVariables.get('raceCourseId')),status,watchedSeconds})}}, (error, res) => {",
    " pm.test('Concurrent ' + status + ' HTTP 200', () => { pm.expect(error).to.eql(null); pm.expect(res.code).to.eql(200); });",
    " });",
    "});",
  ].join("\n") });
for (let n = 1; n <= 2; n++) GET("ENROLL-07.16 Chứng chỉ duy nhất lần " + n,
  "/api/enrollments/" + V("raceEnrollmentId") + "/certificate", 200, "studentToken", eq("d.certificateCode", cv("raceCertificateCode")));
R("SETUP-14 Thêm bài thứ ba sau hoàn thành", "POST", "/api/sections/" + V("mainSectionId") + "/lessons", 201,
  { title: "Bài bổ sung", type: "ARTICLE", content: "Bổ sung sau cấp chứng chỉ", position: 3, durationSeconds: 60 }, "tokenA", save("addedLessonId"));
progressGet("ENROLL-07.17 Đợi snapshot có ba bài", eq("d.totalLessonsCount", 3) + percent(100) + unchangedCertificate,
  { poll: "d && d.totalLessonsCount === 3" });
progress("ENROLL-07.17 Gửi IN_PROGRESS sau khi thêm bài", 200, { status: "IN_PROGRESS", watchedSeconds: 100 }, V("lessonAId"), "studentToken", completedLesson);
progressGet("ENROLL-07.17 Vẫn COMPLETED và 100%", eq("d.totalLessonsCount", 3) + eq("d.status", "'COMPLETED'") + percent(100) + unchangedCertificate);
GET("ENROLL-07.17 Chứng chỉ không đổi", cert, 200, "studentToken", unchangedCertificate + eq("d.id", id("certificateId")));

folder("6. Xóa và reset", "Chạy cuối cùng vì xóa cả chứng chỉ. Chỉ xóa lượt học fixture của chính tài khoản; không xóa khóa có sẵn.");
const del = (name, status, token = "studentToken", path = "/api/enrollments?courseId=" + V("publishedCourseId")) => R(name, "DELETE", path, status, undefined, token);
del("ENROLL-05.2 Không token", 401, null);
del("ENROLL-05.3 A chưa ghi danh không xóa được S", 404, "tokenA");
GET("ENROLL-05.3 S vẫn đọc được lượt", own, 200, "studentToken", eq("d.id", id("enrollmentId")));
del("ENROLL-05.4 Khóa không có lượt", 404, "studentToken", "/api/enrollments?courseId=" + V("missingId"));
del("ENROLL-05.5 ID sai kiểu", 400, "studentToken", "/api/enrollments?courseId=abc");
del("ENROLL-05.6 Thiếu courseId", 400, "studentToken", "/api/enrollments");
del("ENROLL-05.7 B chỉ xóa lượt của B", 200, "tokenB");
GET("ENROLL-05.7 S vẫn còn lượt", own, 200, "studentToken", eq("d.userId", id("studentId")));
del("ENROLL-05.1 Xóa lượt S", 200);
GET("ENROLL-05.1 ID cũ không còn", own, 404);
GET("ENROLL-05.1 Tiến độ đã xóa", "/api/progress?courseId=" + V("publishedCourseId"), 404);
GET("ENROLL-05.1 Chứng chỉ riêng đã xóa", cert, 404);
GET("ENROLL-10.8 Mã chứng chỉ đã xóa không xác minh được", verify, 404, null);
enroll("ENROLL-05.8 Ghi danh lại có ID mới", 201, "studentToken", V("publishedCourseId"),
  "pm.expect(d.id).not.to.eql(id('enrollmentId'));" + percent(0) + eq("d.status", "'ACTIVE'") + save("resetEnrollmentId"));
progressGet("ENROLL-05.8 Không khôi phục tiến độ cũ", percent(0) + eq("d.lessons", "[]") + eq("d.certificateCode", "null"));

folders.push({ name: "7. ENROLL-09 — thao tác hạ tầng thủ công", item: [], description:
  "Không giả PASS bằng HTTP. Năm ca ENROLL-09.1–09.5 cần dừng MySQL, gửi Kafka và làm DLT lỗi trên môi trường thử riêng. Thực hiện đúng docs/test-cases/enrollment.md, khôi phục hạ tầng sau mỗi ca; ghi HTTP là N/A cùng log, offset và message DLT trong biên bản. Runner không thực hiện năm ca này." });
const collection = { info: { name: "Enrollment — gateway, progress and certificates",
  description: "Chạy một iteration theo thứ tự từ 0. Chuẩn bị. Biến collection, không environment, không SQL snapshot. Chỉ dùng dữ liệu dev; tạo khóa mới mỗi lần chạy. ENROLL-09 phải kiểm thủ công và không được tính PASS của Runner. Chi tiết: docs/postman/enrollment.md.",
  schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
  auth: { type: "noauth" }, variable: Object.entries(vars).map(([key, value]) => ({ key, value, type: "string" })), item: folders };
writeFileSync(new URL("../docs/postman/enrollment.postman_collection.json", import.meta.url), JSON.stringify(collection, null, 2) + "\n");
console.log("Generated " + folders.reduce((n, f) => n + f.item.length, 0) + " requests.");
