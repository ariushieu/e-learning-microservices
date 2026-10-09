import { GraduationCapIcon } from "lucide-react";
import type { Metadata } from "next";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { attempt } from "@/components/course/queries";
import { getPublishedCourses, summarizeInstructors } from "@/components/home/catalog-data";
import { InstructorCard } from "@/components/home/sections";
import { ListPage } from "@/components/templates/list-page";
import { formatNumber } from "@/lib/format";

export const metadata: Metadata = { title: "Giảng viên" };

export default async function InstructorsPage() {
  const courses = await attempt(getPublishedCourses());
  const instructors = courses.data ? summarizeInstructors(courses.data) : [];

  return (
    <ListPage
      eyebrow="Đội ngũ giảng dạy"
      title="Giảng viên"
      description={
        courses.data
          ? `${formatNumber(instructors.length)} giảng viên đang mở ${formatNumber(courses.data.length)} khóa học. Bấm vào thẻ để xem hồ sơ và các khóa của từng người.`
          : undefined
      }
    >
      {courses.error !== null ? (
        <ErrorAlert title="Không tải được danh sách giảng viên" message={courses.error} />
      ) : instructors.length === 0 ? (
        <EmptyState icon={GraduationCapIcon} title="Chưa có giảng viên nào mở khóa học" />
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {instructors.map((i) => (
            <InstructorCard key={i.id} instructor={i} />
          ))}
        </div>
      )}
    </ListPage>
  );
}
