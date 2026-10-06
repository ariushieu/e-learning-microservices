import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { CreateQuizButton } from "@/components/quiz/create-quiz-button";
import { CourseForm } from "@/components/instructor/course-form";
import { CourseStatusActions } from "@/components/instructor/course-status-actions";
import { CurriculumBuilder } from "@/components/instructor/curriculum-builder";
import { SectionHeading, StatCard } from "@/components/instructor/form-helpers";
import { Alert, Badge, Card, Empty, LinkButton, formatDate, formatDuration, formatPrice } from "@/components/ui";
import { hasRole } from "@/lib/auth-shared";
import { ApiError, errorMessage } from "@/lib/errors";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Category, Course, Quiz, Section } from "@/lib/types";

export const metadata: Metadata = { title: "Quản lý khóa học" };

export default async function ManageCoursePage({ params }: PageProps<"/instructor/courses/[id]">) {
  const { id } = await params;
  const courseId = Number(id);
  if (!Number.isInteger(courseId) || courseId <= 0) notFound();

  const session = await getSession();
  if (!session) redirect(`/login?next=/instructor/courses/${courseId}`);

  const [courseRes, curriculumRes, quizzesRes, categoriesRes] = await Promise.allSettled([
    gateway<Course>(`/api/courses/${courseId}`),
    gateway<Section[]>(`/api/courses/${courseId}/curriculum`),
    gateway<Quiz[]>(`/api/quizzes?courseId=${courseId}`),
    gateway<Category[]>("/api/categories/tree"),
  ]);

  if (courseRes.status === "rejected") {
    const e = courseRes.reason;
    const status = e instanceof ApiError ? e.status : 0;
    return (
      <Blocked
        title={status === 404 ? "Không tìm thấy khóa học" : status === 403 ? "Bạn không có quyền" : "Không tải được khóa học"}
        message={
          status === 404
            ? "Khóa học không tồn tại, hoặc là bản nháp của giảng viên khác."
            : status === 403
              ? "Chỉ giảng viên của khóa học hoặc quản trị viên mới quản lý được khóa này."
              : errorMessage(e)
        }
      />
    );
  }

  const course = courseRes.value;
  if (course.instructorId !== session.userId && !hasRole(session, "ROLE_ADMIN")) {
    // Khóa đã xuất bản ai cũng GET được, nên phải tự chặn ở đây; backend vẫn trả 403 cho mọi thao tác ghi.
    return (
      <Blocked
        title="Bạn không có quyền"
        message="Đây là khóa học của giảng viên khác. Chỉ giảng viên của khóa hoặc quản trị viên mới quản lý được."
        courseId={course.id}
      />
    );
  }

  const sections = curriculumRes.status === "fulfilled" ? curriculumRes.value : null;
  const quizzes = quizzesRes.status === "fulfilled" ? quizzesRes.value : null;
  const categories = categoriesRes.status === "fulfilled" ? categoriesRes.value : null;
  const archived = course.status === "ARCHIVED";

  return (
    <div className="space-y-8">
      <div className="text-sm">
        <Link href="/instructor" className="text-indigo-600 hover:underline">
          ← Khóa học tôi dạy
        </Link>
      </div>

      {/* a. Đầu trang: trạng thái và thao tác */}
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <Badge value={course.status} />
              <Badge value={course.level} />
              <span className="text-sm text-slate-500">{course.categoryName}</span>
            </div>
            <h1 className="mt-2 text-2xl font-bold text-slate-900">{course.title}</h1>
            <p className="mt-1 text-sm text-slate-500">
              Mã khóa #{course.id} · Tạo {formatDate(course.createdAt)} · Cập nhật {formatDate(course.updatedAt)}
              {course.publishedAt && ` · Xuất bản ${formatDate(course.publishedAt)}`}
            </p>
          </div>
        </div>
        <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-5">
          <StatCard label="Chương" value={sections?.length ?? "—"} />
          <StatCard label="Bài học" value={course.totalLessons} />
          <StatCard label="Thời lượng" value={formatDuration(course.totalDurationSeconds)} />
          <StatCard label="Học viên" value={course.studentCount} />
          <StatCard label="Học phí" value={formatPrice(course.price)} />
        </div>
        <div className="mt-5 border-t border-slate-100 pt-5">
          <CourseStatusActions course={course} />
        </div>
      </Card>

      <nav className="sticky top-[4.5rem] z-10 flex gap-1 overflow-x-auto rounded-xl border border-slate-200 bg-white/90 p-1 text-sm shadow-sm backdrop-blur">
        {[
          ["#thong-tin", "Thông tin"],
          ["#noi-dung", "Nội dung khóa học"],
          ["#bai-kiem-tra", "Bài kiểm tra"],
        ].map(([href, label]) => (
          <a key={href} href={href} className="whitespace-nowrap rounded-lg px-3 py-1.5 font-medium text-slate-600 hover:bg-slate-100">
            {label}
          </a>
        ))}
      </nav>

      {/* b. Thông tin chung */}
      <section>
        <SectionHeading
          id="thong-tin"
          title="Thông tin"
          description="Lưu sẽ ghi đè toàn bộ thông tin khóa học bằng nội dung trong form."
        />
        <Card>
          {archived && (
            <div className="mb-4">
              <Alert kind="info">Khóa học đã lưu trữ nên không sửa được thông tin. Bấm “Mở lại” ở trên để chỉnh sửa.</Alert>
            </div>
          )}
          {categories ? (
            <CourseForm
              categories={categories}
              course={course}
              instructorName={session.fullName}
              disabled={archived}
            />
          ) : (
            <Alert>
              Không tải được danh mục nên chưa sửa được thông tin:{" "}
              {categoriesRes.status === "rejected" ? errorMessage(categoriesRes.reason) : ""}
            </Alert>
          )}
        </Card>
      </section>

      {/* c. Đề cương: chương, bài học, tài liệu */}
      <section>
        <SectionHeading
          id="noi-dung"
          title="Nội dung khóa học"
          description="Sắp xếp theo số thứ tự (nhỏ hiện trước). Bài “xem thử” ai cũng xem được nội dung, không cần ghi danh."
        />
        {sections ? (
          <CurriculumBuilder courseId={course.id} sections={sections} />
        ) : (
          <Alert>
            Không tải được đề cương: {curriculumRes.status === "rejected" ? errorMessage(curriculumRes.reason) : ""}
          </Alert>
        )}
      </section>

      {/* d. Bài kiểm tra */}
      <section>
        <SectionHeading
          id="bai-kiem-tra"
          title="Bài kiểm tra"
          description="Học viên chỉ thấy bài kiểm tra đã xuất bản. Soạn câu hỏi xong mới xuất bản được."
        />
        <div className="space-y-4">
          {quizzes === null ? (
            <Alert>
              Không tải được bài kiểm tra: {quizzesRes.status === "rejected" ? errorMessage(quizzesRes.reason) : ""}
            </Alert>
          ) : quizzes.length === 0 ? (
            <Empty>Khóa học chưa có bài kiểm tra nào.</Empty>
          ) : (
            <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
              {quizzes.map((q) => (
                <li key={q.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-medium text-slate-900">{q.title}</span>
                      <Badge value={q.status} />
                    </div>
                    <div className="mt-1 flex flex-wrap gap-x-3 text-xs text-slate-500">
                      <span>{q.totalQuestions} câu hỏi</span>
                      <span>Điểm đạt {q.passScore}/100</span>
                      <span>{q.maxAttempts === 0 ? "Không giới hạn lượt" : `Tối đa ${q.maxAttempts} lượt`}</span>
                      <span>{q.timeLimitMinutes ? `${q.timeLimitMinutes} phút` : "Không giới hạn thời gian"}</span>
                    </div>
                  </div>
                  <LinkButton href={`/instructor/quizzes/${q.id}`} variant="secondary" className="px-3 py-1.5">
                    Soạn đề
                  </LinkButton>
                </li>
              ))}
            </ul>
          )}
          <CreateQuizButton courseId={course.id} />
        </div>
      </section>
    </div>
  );
}

function Blocked({ title, message, courseId }: { title: string; message: string; courseId?: number }) {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
      <p className="mt-2 text-slate-500">{message}</p>
      <div className="mt-6 flex justify-center gap-2">
        <LinkButton href="/instructor">Về khóa học của tôi</LinkButton>
        {courseId && (
          <LinkButton href={`/courses/${courseId}`} variant="secondary">
            Xem trang khóa
          </LinkButton>
        )}
      </div>
    </div>
  );
}
