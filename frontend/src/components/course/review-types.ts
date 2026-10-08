export interface CourseReview {
  id: number;
  rating: number;
  comment: string | null;
  authorName: string;
  createdAt: string;
  updatedAt: string;
  reply: string | null;
  repliedAt: string | null;
  replyAuthorRole: "INSTRUCTOR" | "ADMIN" | null;
}

export interface InstructorReviews {
  reviews: import("@/lib/types").Page<{ courseId: number; courseTitle: string; review: CourseReview }>;
  unrepliedCount: number;
  courses: { id: number; title: string }[];
}

export interface MyCourseReview {
  canReview: boolean;
  review: CourseReview | null;
}
