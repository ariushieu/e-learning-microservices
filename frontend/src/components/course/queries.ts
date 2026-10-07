import "server-only";

import { cache } from "react";
import { errorMessage } from "@/lib/errors";
import { gateway, gatewayOrNull } from "@/lib/server/gateway";
import type { Course, Enrollment, Page } from "@/lib/types";

/** Id trên URL phải là số dương; chuỗi khác thì coi như không tồn tại, khỏi gọi backend. */
export function isId(value: string): boolean {
  return /^\d{1,18}$/.test(value);
}

/** Dùng chung cho generateMetadata và trang trong cùng một lượt render, chỉ gọi API một lần. */
export const getCourse = cache((id: string) => gatewayOrNull<Course>(`/api/courses/${id}`));

export const getMyEnrollments = cache(async (): Promise<Enrollment[]> => {
  const page = await gateway<Page<Enrollment>>("/api/enrollments?size=100");
  return page.content;
});

export type Attempt<T> = { data: T; error: null } | { data: null; error: string };

/** Lỗi ở một khối phụ (danh mục, bài kiểm tra...) không được làm hỏng cả trang. */
export async function attempt<T>(promise: Promise<T>): Promise<Attempt<T>> {
  try {
    return { data: await promise, error: null };
  } catch (e) {
    return { data: null, error: errorMessage(e) };
  }
}
