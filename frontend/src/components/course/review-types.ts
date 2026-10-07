export interface CourseReview {
  id: number;
  rating: number;
  comment: string | null;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface MyCourseReview {
  canReview: boolean;
  review: CourseReview | null;
}
