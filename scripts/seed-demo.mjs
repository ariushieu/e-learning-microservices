#!/usr/bin/env node
//
// Tạo dữ liệu mẫu cho buổi demo, đi qua gateway như người dùng thật — không chèn SQL, nên
// mọi sự kiện Kafka (đồng bộ khóa học sang enrollment-service) đều chạy đúng như thường.
//
//   docker compose --profile app up -d --build --wait
//   node scripts/seed-demo.mjs
//
// Đổi địa chỉ gateway: GATEWAY=http://localhost:9000 node scripts/seed-demo.mjs
//
// Chạy lại nhiều lần không sinh trùng: tài khoản đã có thì đăng nhập, khóa học trùng tên của
// cùng giảng viên thì bỏ qua.
//
// Nếu gateway đang bật giới hạn request, script đăng nhập vài lần là chạm ngưỡng 10 lần/phút.
// Gặp 429 thì script tự chờ rồi thử lại.

const GATEWAY = (process.env.GATEWAY ?? "http://localhost:8080").replace(/\/+$/, "");
const PASSWORD = "Demo@123456";

const ADMIN = { email: "admin@elearning.hunre.edu.vn", password: "Admin@123456" };
const INSTRUCTOR = { email: "giangvien@hunre.edu.vn", fullName: "TS. Nguyễn Văn An", password: PASSWORD };
const STUDENT = { email: "hocvien@hunre.edu.vn", fullName: "Trần Thị Bình", password: PASSWORD };

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function call(method, path, { token, body } = {}) {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch(GATEWAY + path, {
      method,
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (res.status === 429 && attempt < 10) {
      const wait = Number(res.headers.get("retry-after") ?? 6);
      console.log(`  … gateway giới hạn request, chờ ${wait} giây`);
      await sleep(wait * 1000);
      continue;
    }
    const text = await res.text();
    const json = text ? JSON.parse(text) : null;
    if (!res.ok) {
      const err = new Error(`${method} ${path} → ${res.status} ${json?.code ?? ""} ${json?.message ?? ""}`);
      err.status = res.status;
      throw err;
    }
    return json?.data;
  }
}

async function login({ email, password }) {
  return call("POST", "/api/auth/login", { body: { email, password } });
}

async function ensureUser(user) {
  try {
    await call("POST", "/api/auth/register", { body: user });
    console.log(`+ tài khoản ${user.email}`);
  } catch (e) {
    if (e.status !== 409) throw e;
    console.log(`= tài khoản ${user.email} đã có`);
  }
  return login(user);
}

// Giống cách course-service sinh slug: bỏ dấu tiếng Việt, chữ thường, nối bằng gạch ngang.
const slugify = (text) =>
  text.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D")
    .toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

async function ensureCategory(token, existing, name, parentId) {
  // So theo slug chứ không theo tên: database dev có thể đã có "Cong nghe thong tin" không dấu.
  const found = existing.find((c) => c.slug === slugify(name));
  if (found) return found;
  const created = await call("POST", "/api/categories", { token, body: { name, parentId } });
  console.log(`+ danh mục ${name}`);
  existing.push(created);
  return created;
}

const COURSES = [
  {
    category: ["Công nghệ thông tin", "Kiến trúc phần mềm"],
    title: "Kiến trúc Microservices với Spring Boot",
    summary: "Thiết kế, xây dựng và vận hành hệ thống microservices: API Gateway, Kafka, Redis, Docker.",
    description:
      "Khóa học đi từ nguyên lý tới thực hành trên chính hệ thống E-Learning này.\n\n" +
      "Bạn sẽ học:\n• Chia service theo nghiệp vụ, mỗi service một database\n• API Gateway, xác thực JWT\n" +
      "• Giao tiếp bất đồng bộ qua Kafka, outbox pattern\n• Giới hạn request bằng Redis\n• Đóng gói và chạy bằng Docker Compose",
    level: "INTERMEDIATE",
    price: 0,
    sections: [
      {
        title: "Chương 1. Tổng quan",
        lessons: [
          { title: "Microservices là gì?", type: "VIDEO", contentUrl: "https://www.youtube.com/watch?v=lL_j7ilk7rc", durationSeconds: 660, isPreview: true },
          {
            title: "So sánh với kiến trúc nguyên khối",
            type: "ARTICLE",
            durationSeconds: 420,
            content:
              "Ứng dụng nguyên khối (monolith) gói mọi chức năng vào một khối triển khai.\n\n" +
              "Microservices tách thành nhiều service nhỏ, mỗi service:\n- sở hữu dữ liệu riêng\n- triển khai độc lập\n- giao tiếp qua API hoặc sự kiện\n\n" +
              "Đổi lại là độ phức tạp vận hành: mạng, nhất quán dữ liệu, giám sát.",
          },
        ],
      },
      {
        title: "Chương 2. Giao tiếp giữa các service",
        lessons: [
          { title: "REST qua API Gateway", type: "VIDEO", contentUrl: "https://www.youtube.com/watch?v=6ULyxuHKxg8", durationSeconds: 540 },
          {
            title: "Sự kiện với Kafka và outbox pattern",
            type: "ARTICLE",
            durationSeconds: 600,
            content:
              "Ghi sự kiện vào bảng outbox trong cùng transaction với dữ liệu nghiệp vụ, rồi một worker gửi lên Kafka.\n\n" +
              "Kafka chết thì sự kiện nằm chờ trong bảng, không mất. Phía nhận dùng bảng processed_events để không xử lý trùng.",
          },
        ],
      },
    ],
    quiz: {
      title: "Kiểm tra chương 1–2",
      description: "5 câu hỏi về kiến trúc microservices.",
      timeLimitMinutes: 10,
      passScore: 60,
      maxAttempts: 3,
      questions: [
        { content: "Mỗi microservice nên sở hữu database riêng.", type: "TRUE_FALSE", options: [["Đúng", true], ["Sai", false]] },
        { content: "Thành phần nào là cổng vào duy nhất của hệ thống?", type: "SINGLE_CHOICE", options: [["API Gateway", true], ["Kafka", false], ["Redis", false], ["MySQL", false]] },
        { content: "Outbox pattern giải quyết vấn đề gì?", type: "SINGLE_CHOICE", options: [["Mất sự kiện khi message broker chết", true], ["Tăng tốc truy vấn", false], ["Giảm dung lượng ảnh", false]] },
        { content: "Chọn các công nghệ dùng trong hệ thống này.", type: "MULTIPLE_CHOICE", options: [["Kafka", true], ["Redis", true], ["MongoDB", false], ["Spring Boot", true]] },
        { content: "Redis trong hệ thống dùng để làm gì?", type: "SINGLE_CHOICE", options: [["Giới hạn số request", true], ["Lưu khóa học", false], ["Gửi email", false]] },
      ],
    },
  },
  {
    category: ["Công nghệ thông tin", "Cơ sở dữ liệu"],
    title: "SQL căn bản cho người mới bắt đầu",
    summary: "Truy vấn, lọc, nhóm và nối bảng với MySQL qua ví dụ thực tế.",
    description: "Không cần biết trước về cơ sở dữ liệu. Mỗi bài có ví dụ chạy được trên MySQL 8.",
    level: "BEGINNER",
    price: 199000,
    sections: [
      {
        title: "Chương 1. Làm quen với SQL",
        lessons: [
          { title: "Cơ sở dữ liệu quan hệ là gì", type: "VIDEO", contentUrl: "https://www.youtube.com/watch?v=OqjJjpjDRLc", durationSeconds: 480, isPreview: true },
          { title: "Câu lệnh SELECT", type: "ARTICLE", durationSeconds: 360, content: "SELECT cot1, cot2 FROM bang WHERE dieu_kien ORDER BY cot1;\n\nVí dụ: SELECT title, price FROM courses WHERE status = 'PUBLISHED';" },
          { title: "GROUP BY và hàm tổng hợp", type: "ARTICLE", durationSeconds: 420, content: "COUNT, SUM, AVG, MIN, MAX đi cùng GROUP BY.\n\nSELECT category_id, COUNT(*) FROM courses GROUP BY category_id;" },
        ],
      },
    ],
    quiz: {
      title: "Bài kiểm tra SQL cơ bản",
      description: "3 câu, không giới hạn thời gian.",
      timeLimitMinutes: null,
      passScore: 50,
      maxAttempts: 0,
      questions: [
        { content: "Mệnh đề nào dùng để lọc dòng?", type: "SINGLE_CHOICE", options: [["WHERE", true], ["ORDER BY", false], ["SELECT", false]] },
        { content: "COUNT(*) đếm số dòng.", type: "TRUE_FALSE", options: [["Đúng", true], ["Sai", false]] },
        { content: "Hàm nào là hàm tổng hợp?", type: "MULTIPLE_CHOICE", options: [["SUM", true], ["AVG", true], ["UPPER", false]] },
      ],
    },
  },
  {
    category: ["Kỹ năng", null],
    title: "Kỹ năng làm việc nhóm trong dự án phần mềm",
    summary: "Git, chia việc, review code và giao tiếp hiệu quả trong nhóm 5 người.",
    description: "Rút ra từ chính quá trình làm sản phẩm môn học của nhóm.",
    level: "BEGINNER",
    price: 0,
    sections: [
      {
        title: "Chương 1. Làm việc với Git",
        lessons: [
          { title: "Nhánh, pull request và review", type: "VIDEO", contentUrl: "https://www.youtube.com/watch?v=8JJ101D3knE", durationSeconds: 600, isPreview: true },
          { title: "Viết commit message tử tế", type: "ARTICLE", durationSeconds: 300, content: "<loại>(<phạm vi>): <mô tả ngắn bằng tiếng Anh>\n\nVí dụ: feat(course): enforce curriculum ownership" },
        ],
      },
    ],
  },
];

async function main() {
  console.log(`Gateway: ${GATEWAY}`);
  const admin = await login(ADMIN);
  const instructor = await ensureUser(INSTRUCTOR);
  const student = await ensureUser(STUDENT);

  if (!instructor.user.roles.includes("ROLE_INSTRUCTOR")) {
    await call("PATCH", `/api/users/${instructor.user.id}/roles`, {
      token: admin.accessToken,
      body: { roles: ["ROLE_STUDENT", "ROLE_INSTRUCTOR"] },
    });
    console.log(`+ cấp quyền giảng viên cho ${INSTRUCTOR.email}`);
  }
  // Đăng nhập lại để token mang vai trò giảng viên.
  const teacher = await login(INSTRUCTOR);
  const token = teacher.accessToken;

  const categories = (await call("GET", "/api/categories?size=200")).content;
  const mine = (await call("GET", `/api/courses?instructorId=${teacher.user.id}&size=100`, { token })).content;

  for (const spec of COURSES) {
    const root = await ensureCategory(admin.accessToken, categories, spec.category[0], null);
    const category = spec.category[1] ? await ensureCategory(admin.accessToken, categories, spec.category[1], root.id) : root;

    if (mine.some((c) => c.title === spec.title)) {
      console.log(`= khóa "${spec.title}" đã có`);
      continue;
    }

    const course = await call("POST", "/api/courses", {
      token,
      body: {
        categoryId: category.id,
        title: spec.title,
        summary: spec.summary,
        description: spec.description,
        level: spec.level,
        price: spec.price,
        language: "vi",
        instructorName: INSTRUCTOR.fullName,
      },
    });
    for (const [si, section] of spec.sections.entries()) {
      const s = await call("POST", `/api/courses/${course.id}/sections`, { token, body: { title: section.title, position: si } });
      for (const [li, lesson] of section.lessons.entries()) {
        await call("POST", `/api/sections/${s.id}/lessons`, { token, body: { ...lesson, position: li } });
      }
    }
    // Xuất bản sau cùng: course.updated mang đúng tổng số bài sang enrollment-service.
    await call("PATCH", `/api/courses/${course.id}/status`, { token, body: { status: "PUBLISHED" } });
    console.log(`+ khóa "${spec.title}" (id ${course.id}) đã xuất bản`);

    if (spec.quiz) {
      const { questions, ...quizBody } = spec.quiz;
      const quiz = await call("POST", "/api/quizzes", { token, body: { ...quizBody, courseId: course.id } });
      for (const [qi, q] of questions.entries()) {
        await call("POST", `/api/quizzes/${quiz.id}/questions`, {
          token,
          body: {
            content: q.content,
            type: q.type,
            score: 1,
            position: qi,
            options: q.options.map(([content, isCorrect], oi) => ({ content, isCorrect, position: oi })),
          },
        });
      }
      await call("PATCH", `/api/quizzes/${quiz.id}/status`, { token, body: { status: "PUBLISHED" } });
      console.log(`  + bài kiểm tra "${spec.quiz.title}" (${questions.length} câu)`);
    }
  }

  console.log("\nXong. Tài khoản demo (mật khẩu chung " + PASSWORD + "):");
  console.log(`  Giảng viên: ${INSTRUCTOR.email}`);
  console.log(`  Học viên:   ${STUDENT.email}   (id ${student.user.id})`);
  console.log(`  Admin:      ${ADMIN.email} / ${ADMIN.password}`);
}

main().catch((e) => {
  console.error("LỖI:", e.message);
  process.exit(1);
});
