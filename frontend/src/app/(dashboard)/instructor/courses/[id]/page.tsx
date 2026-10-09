import {
  ArchiveIcon,
  ClipboardListIcon,
  ClockIcon,
  InfoIcon,
  LayersIcon,
  ListVideoIcon,
  PencilRulerIcon,
  UsersIcon,
} from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { Callout } from "@/components/common/callout";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Section } from "@/components/common/section";
import { Stat } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { StatusPage } from "@/components/common/status-page";
import { AnnouncementComposer, AnnouncementList } from "@/components/course/course-announcements";
import { CourseForm } from "@/components/course/course-form";
import { CourseStatusActions } from "@/components/course/course-status-actions";
import { CurriculumBuilder } from "@/components/course/curriculum-builder";
import { CourseLearners } from "@/components/enrollment/course-learners";
import { CreateQuizButton } from "@/components/quiz/create-quiz-button";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { UrlTabs } from "@/components/common/url-tabs";
import { TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { hasRole } from "@/lib/auth-shared";
import { ApiError, errorMessage } from "@/lib/errors";
import { formatDay, formatDuration, formatNumber, formatPrice, label } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Category, Course, CourseAnnouncement, Page, Quiz, Section as CourseSection } from "@/lib/types";

export const metadata: Metadata = { title: "Quản lý khóa học" };

export default async function ManageCoursePage({ params }: PageProps<"/instructor/courses/[id]">) {
  const { id } = await params;
  const courseId = Number(id);
  if (!Number.isInteger(courseId) || courseId <= 0) notFound();

  const session = await getSession();
  if (!session) redirect(`/login?next=/instructor/courses/${courseId}`);

  const [courseRes, curriculumRes, quizzesRes, categoriesRes, announcementsRes] = await Promise.allSettled([
    gateway<Course>(`/api/courses/${courseId}`),
    gateway<CourseSection[]>(`/api/courses/${courseId}/curriculum`),
    gateway<Quiz[]>(`/api/quizzes?courseId=${courseId}`),
    gateway<Category[]>("/api/categories/tree"),
    gateway<Page<CourseAnnouncement>>(`/api/courses/${courseId}/announcements?size=20`),
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
  const announcements = announcementsRes.status === "fulfilled" ? announcementsRes.value : null;
  const archived = course.status === "ARCHIVED";
  const isDraft = course.status === "DRAFT" || course.status === "PENDING_REVIEW";

  return (
    <DashboardPage
      crumbs={[{ href: "/instructor", label: "Khóa học tôi dạy" }, { label: course.title }]}
      title={course.title}
      description={
        <>
          <span className="mb-2 flex flex-wrap items-center gap-2">
            <StatusBadge status={course.status} />
            <Badge variant="secondary">{label(course.level)}</Badge>
            <span className="text-sm">{course.categoryName}</span>
          </span>
          <span className="block text-sm">
            {formatPrice(course.price)} · Tạo {formatDay(course.createdAt)} · Cập nhật {formatDay(course.updatedAt)}
            {course.publishedAt && ` · Xuất bản ${formatDay(course.publishedAt)}`}
          </span>
        </>
      }
      actions={<CourseStatusActions course={course} />}
      stats={
        <>
          <Stat label="Chương" value={sections?.length ?? "—"} icon={LayersIcon} tone="primary" />
          <Stat label="Bài học" value={course.totalLessons} icon={ListVideoIcon} tone="primary" />
          <Stat label="Thời lượng" value={formatDuration(course.totalDurationSeconds)} icon={ClockIcon} tone="neutral" />
          <Stat label="Học viên" value={formatNumber(course.studentCount)} icon={UsersIcon} tone="info" />
        </>
      }
    >
      {isDraft && course.totalLessons === 0 && (
        <Callout icon={InfoIcon} tone="info" title="Khóa học chưa có bài học nào">
          Nên thêm nội dung ở tab “Nội dung” trước khi xuất bản.
        </Callout>
      )}

      <UrlTabs values={["noi-dung", "hoc-vien", "thong-bao", "bai-kiem-tra", "thong-tin"]} defaultValue="noi-dung" className="gap-6">
        {/* Các tab không vừa màn 375px: cuộn ngang trong khung thay vì làm tràn cả trang. */}
        <div className="max-w-full overflow-x-auto">
        <TabsList>
          <TabsTrigger value="noi-dung" className="px-3">
            Nội dung
          </TabsTrigger>
          <TabsTrigger value="hoc-vien" className="px-3">
            Học viên
            <span className="text-muted-foreground tabular-nums">({formatNumber(course.studentCount)})</span>
          </TabsTrigger>
          <TabsTrigger value="thong-bao" className="px-3">
            Thông báo
            {announcements && announcements.totalElements > 0 && (
              <span className="text-muted-foreground tabular-nums">({formatNumber(announcements.totalElements)})</span>
            )}
          </TabsTrigger>
          <TabsTrigger value="bai-kiem-tra" className="px-3">
            Bài kiểm tra
            {quizzes && quizzes.length > 0 && <span className="text-muted-foreground tabular-nums">({quizzes.length})</span>}
          </TabsTrigger>
          <TabsTrigger value="thong-tin" className="px-3">
            Thông tin
          </TabsTrigger>
        </TabsList>
        </div>

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

        <TabsContent value="hoc-vien">
          <CourseLearners courseId={course.id} sections={sections} />
        </TabsContent>

        <TabsContent value="thong-bao" className="space-y-6">
          {isDraft ? (
            <Callout icon={InfoIcon} tone="info" title="Xuất bản khóa học để gửi thông báo">
              Khóa nháp chưa có học viên. Sau khi xuất bản, bạn gửi thông báo cho mọi người đã ghi danh tại đây.
            </Callout>
          ) : (
            <AnnouncementComposer courseId={course.id} learnerCount={course.studentCount} />
          )}
          <Section title="Đã gửi" count={announcements?.totalElements}>
            {announcements ? (
              <AnnouncementList courseId={course.id} announcements={announcements.content} canManage />
            ) : (
              <ErrorAlert
                title="Không tải được thông báo"
                message={announcementsRes.status === "rejected" ? errorMessage(announcementsRes.reason) : ""}
              />
            )}
          </Section>
        </TabsContent>

        <TabsContent value="thong-tin" className="space-y-6">
          {archived && (
            <Callout icon={ArchiveIcon} tone="neutral" title="Khóa học đã lưu trữ">
              Không sửa được thông tin. Bấm “Mở lại” ở đầu trang để chỉnh sửa.
            </Callout>
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

        <TabsContent value="bai-kiem-tra">
          <Section
            title="Bài kiểm tra"
            description="Học viên chỉ thấy bài kiểm tra đã xuất bản. Soạn câu hỏi xong mới xuất bản được."
            actions={<CreateQuizButton courseId={course.id} />}
          >
            {quizzes === null ? (
              <ErrorAlert
                title="Không tải được bài kiểm tra"
                message={quizzesRes.status === "rejected" ? errorMessage(quizzesRes.reason) : ""}
              />
            ) : (
              <DataTableCard
                isEmpty={quizzes.length === 0}
                empty={<EmptyState icon={ClipboardListIcon} title="Khóa học chưa có bài kiểm tra nào" description="Tạo bài kiểm tra rồi soạn câu hỏi." />}
              >
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Bài kiểm tra</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead className="hidden text-right sm:table-cell">Câu hỏi</TableHead>
                      <TableHead className="hidden text-right sm:table-cell">Điểm đạt</TableHead>
                      <TableHead className="hidden md:table-cell">Lượt làm</TableHead>
                      <TableHead className="hidden md:table-cell">Thời gian</TableHead>
                      <TableHead className="text-right">
                        <span className="sr-only">Thao tác</span>
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {quizzes.map((q) => (
                      <TableRow key={q.id}>
                        <TableCell className="max-w-72 truncate py-3 font-medium">{q.title}</TableCell>
                        <TableCell>
                          <StatusBadge status={q.status} />
                        </TableCell>
                        <TableCell className="hidden text-right tabular-nums sm:table-cell">{q.totalQuestions}</TableCell>
                        <TableCell className="hidden text-right tabular-nums sm:table-cell">{Number(q.passScore)}/100</TableCell>
                        <TableCell className="hidden text-muted-foreground md:table-cell">
                          {q.maxAttempts === 0 ? "Không giới hạn" : `Tối đa ${q.maxAttempts}`}
                        </TableCell>
                        <TableCell className="hidden text-muted-foreground md:table-cell">
                          {q.timeLimitMinutes ? `${q.timeLimitMinutes} phút` : "Không giới hạn"}
                        </TableCell>
                        <TableCell className="text-right">
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
              </DataTableCard>
            )}
          </Section>
        </TabsContent>
      </UrlTabs>
    </DashboardPage>
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
