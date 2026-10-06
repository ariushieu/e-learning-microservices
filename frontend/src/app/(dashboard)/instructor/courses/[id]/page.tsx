import { ClipboardListIcon, ClockIcon, InfoIcon, LayersIcon, ListVideoIcon, PencilRulerIcon, UsersIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Stat } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { StatusPage } from "@/components/common/status-page";
import { CourseForm } from "@/components/course/course-form";
import { CourseStatusActions } from "@/components/course/course-status-actions";
import { CurriculumBuilder } from "@/components/course/curriculum-builder";
import { CreateQuizButton } from "@/components/quiz/create-quiz-button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { hasRole } from "@/lib/auth-shared";
import { ApiError, errorMessage } from "@/lib/errors";
import { formatDay, formatDuration, formatNumber, formatPrice, label } from "@/lib/format";
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
        code={status === 404 ? "404" : status === 403 ? "403" : "Lỗi"}
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
        code="403"
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
  const isDraft = course.status === "DRAFT" || course.status === "PENDING_REVIEW";

  return (
    <div className="space-y-6">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/instructor">Khóa học tôi dạy</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage className="line-clamp-1">{course.title}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={course.status} />
            <Badge variant="secondary">{label(course.level)}</Badge>
            <span className="text-sm text-muted-foreground">{course.categoryName}</span>
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-balance">{course.title}</h1>
          <p className="text-sm text-muted-foreground">
            Mã khóa #{course.id} · {formatPrice(course.price)} · Tạo {formatDay(course.createdAt)} · Cập nhật {formatDay(course.updatedAt)}
            {course.publishedAt && ` · Xuất bản ${formatDay(course.publishedAt)}`}
          </p>
        </div>
        <CourseStatusActions course={course} />
      </div>

      {isDraft && course.totalLessons === 0 && (
        <Alert>
          <InfoIcon />
          <AlertTitle>Khóa học chưa có bài học nào</AlertTitle>
          <AlertDescription>Nên thêm nội dung ở tab “Nội dung” trước khi xuất bản.</AlertDescription>
        </Alert>
      )}

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Stat label="Chương" value={sections?.length ?? "—"} icon={LayersIcon} />
        <Stat label="Bài học" value={course.totalLessons} icon={ListVideoIcon} />
        <Stat label="Thời lượng" value={formatDuration(course.totalDurationSeconds)} icon={ClockIcon} />
        <Stat label="Học viên" value={formatNumber(course.studentCount)} icon={UsersIcon} />
      </div>

      <Tabs defaultValue="noi-dung" className="gap-6">
        <TabsList>
          <TabsTrigger value="noi-dung" className="px-3">
            Nội dung
          </TabsTrigger>
          <TabsTrigger value="thong-tin" className="px-3">
            Thông tin
          </TabsTrigger>
          <TabsTrigger value="bai-kiem-tra" className="px-3">
            Bài kiểm tra
            {quizzes && quizzes.length > 0 && <span className="text-muted-foreground tabular-nums">({quizzes.length})</span>}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="noi-dung">
          {sections ? (
            <CurriculumBuilder courseId={course.id} sections={sections} />
          ) : (
            <ErrorAlert
              title="Không tải được đề cương"
              message={curriculumRes.status === "rejected" ? errorMessage(curriculumRes.reason) : ""}
            />
          )}
        </TabsContent>

        <TabsContent value="thong-tin" className="space-y-4">
          {archived && (
            <Alert>
              <InfoIcon />
              <AlertTitle>Khóa học đã lưu trữ</AlertTitle>
              <AlertDescription>Không sửa được thông tin. Bấm “Mở lại” ở đầu trang để chỉnh sửa.</AlertDescription>
            </Alert>
          )}
          <p className="text-sm text-muted-foreground">Lưu sẽ ghi đè toàn bộ thông tin khóa học bằng nội dung trong form.</p>
          {categories ? (
            <CourseForm categories={categories} course={course} instructorName={session.fullName} disabled={archived} />
          ) : (
            <ErrorAlert
              title="Không tải được danh mục nên chưa sửa được thông tin"
              message={categoriesRes.status === "rejected" ? errorMessage(categoriesRes.reason) : ""}
            />
          )}
        </TabsContent>

        <TabsContent value="bai-kiem-tra" className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Học viên chỉ thấy bài kiểm tra đã xuất bản. Soạn câu hỏi xong mới xuất bản được.
          </p>
          {quizzes === null ? (
            <ErrorAlert
              title="Không tải được bài kiểm tra"
              message={quizzesRes.status === "rejected" ? errorMessage(quizzesRes.reason) : ""}
            />
          ) : quizzes.length === 0 ? (
            <EmptyState icon={ClipboardListIcon} title="Khóa học chưa có bài kiểm tra nào" description="Tạo bài kiểm tra rồi soạn câu hỏi." />
          ) : (
            <Card className="gap-0 py-0">
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="pl-4">Bài kiểm tra</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Câu hỏi</TableHead>
                    <TableHead className="text-right">Điểm đạt</TableHead>
                    <TableHead>Lượt làm</TableHead>
                    <TableHead>Thời gian</TableHead>
                    <TableHead className="pr-4 text-right">
                      <span className="sr-only">Thao tác</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {quizzes.map((q) => (
                    <TableRow key={q.id}>
                      <TableCell className="max-w-72 truncate py-3 pl-4 font-medium">{q.title}</TableCell>
                      <TableCell>
                        <StatusBadge status={q.status} />
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{q.totalQuestions}</TableCell>
                      <TableCell className="text-right tabular-nums">{Number(q.passScore)}/100</TableCell>
                      <TableCell className="text-muted-foreground">
                        {q.maxAttempts === 0 ? "Không giới hạn" : `Tối đa ${q.maxAttempts}`}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {q.timeLimitMinutes ? `${q.timeLimitMinutes} phút` : "Không giới hạn"}
                      </TableCell>
                      <TableCell className="pr-4 text-right">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/instructor/quizzes/${q.id}`}>
                            <PencilRulerIcon /> Soạn đề
                          </Link>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Card>
          )}
          <CreateQuizButton courseId={course.id} />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function Blocked({ code, title, message, courseId }: { code: string; title: string; message: string; courseId?: number }) {
  return (
    <StatusPage
      code={code}
      title={title}
      description={message}
      action={
        <div className="flex flex-wrap justify-center gap-2">
          <Button asChild size="lg">
            <Link href="/instructor">Về khóa học của tôi</Link>
          </Button>
          {courseId && (
            <Button asChild variant="outline" size="lg">
              <Link href={`/courses/${courseId}`}>Xem trang khóa</Link>
            </Button>
          )}
        </div>
      }
    />
  );
}
