import "server-only";
import { cache } from "react";
import { gateway } from "@/lib/server/gateway";
import type { Category, CourseSummary, Page } from "@/lib/types";

/** Mọi khóa đã xuất bản (danh mục còn nhỏ nên lấy một lần, dùng cho trang chủ và trang giảng viên). */
export const getPublishedCourses = cache(async (): Promise<CourseSummary[]> => {
  const page = await gateway<Page<CourseSummary>>("/api/courses?size=100&sort=createdAt,desc&sort=id,desc");
  return page.content;
});

export const getCategoryTree = cache(() => gateway<Category[]>("/api/categories/tree"));

export interface InstructorSummary {
  id: number;
  name: string;
  courses: CourseSummary[];
  students: number;
  ratingCount: number;
  /** Trung bình có trọng số theo số lượt đánh giá; null khi chưa có đánh giá. */
  ratingAvg: number | null;
}

/** Gom giảng viên từ danh sách khóa công khai: không lộ giảng viên chỉ có khóa nháp. */
export function summarizeInstructors(courses: CourseSummary[]): InstructorSummary[] {
  const byId = new Map<number, InstructorSummary & { weighted: number }>();
  for (const c of courses) {
    const item = byId.get(c.instructorId) ?? {
      id: c.instructorId, name: c.instructorName || "Giảng viên HUNRE", courses: [], students: 0, ratingCount: 0, ratingAvg: null, weighted: 0,
    };
    item.courses.push(c);
    item.students += c.studentCount;
    item.ratingCount += c.ratingCount;
    item.weighted += Number(c.ratingAvg) * c.ratingCount;
    byId.set(c.instructorId, item);
  }
  return [...byId.values()]
    .map(({ weighted, ...i }) => ({ ...i, ratingAvg: i.ratingCount > 0 ? weighted / i.ratingCount : null }))
    .sort((a, b) => b.students - a.students || b.courses.length - a.courses.length);
}

export interface PlatformStats {
  courses: number;
  instructors: number;
  enrollments: number;
  reviews: number;
  ratingAvg: number | null;
}

export function platformStats(courses: CourseSummary[]): PlatformStats {
  const reviews = courses.reduce((n, c) => n + c.ratingCount, 0);
  const weighted = courses.reduce((n, c) => n + Number(c.ratingAvg) * c.ratingCount, 0);
  return {
    courses: courses.length,
    instructors: new Set(courses.map((c) => c.instructorId)).size,
    enrollments: courses.reduce((n, c) => n + c.studentCount, 0),
    reviews,
    ratingAvg: reviews > 0 ? weighted / reviews : null,
  };
}

/** Khóa nổi bật: nhiều đánh giá tốt và nhiều học viên trước. */
export function featuredCourses(courses: CourseSummary[], limit = 6): CourseSummary[] {
  const score = (c: CourseSummary) => Number(c.ratingAvg) * c.ratingCount * 2 + c.studentCount + (c.thumbnailUrl ? 1 : 0);
  return [...courses].sort((a, b) => score(b) - score(a)).slice(0, limit);
}

/** Đếm khóa thuộc một danh mục cha, gồm cả danh mục con. */
export function countInCategory(category: Category, courses: CourseSummary[]): number {
  const ids = new Set([category.id, ...category.subCategories.map((c) => c.id)]);
  return courses.filter((c) => ids.has(c.categoryId)).length;
}
