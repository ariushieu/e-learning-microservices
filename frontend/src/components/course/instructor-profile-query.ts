import "server-only";

import { cache } from "react";
import { ApiError } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";

export interface InstructorProfile {
  name: string;
  publishedCourses: number;
  totalStudents: number;
  ratingAvg: number;
  ratingCount: number;
}

// Dữ liệu công khai giống nhau với mọi người xem; cache chỉ trong lượt render.
export const getInstructorProfile = cache(async (id: string): Promise<InstructorProfile | null> => {
  try {
    return await gateway<InstructorProfile>(`/api/instructors/${id}`, { anonymous: true });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
});
