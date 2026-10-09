// Kiểu dữ liệu khớp đúng DTO của backend (xem docs/shared-contracts.md và controller từng service).
// Đổi DTO bên Java thì sửa ở đây cùng lúc.

export type Role = "ROLE_STUDENT" | "ROLE_INSTRUCTOR" | "ROLE_ADMIN";

export interface ApiEnvelope<T> {
  success: boolean;
  message: string | null;
  data: T;
  timestamp: string;
}

export interface ErrorBody {
  success: false;
  code: string;
  message: string;
  path?: string;
  fieldErrors?: { field: string; message: string }[];
}

/** Thông báo giảng viên gửi cho học viên của khóa. recipientCount chỉ có khi người xem quản lý khóa. */
export interface CourseAnnouncement {
  id: number;
  courseId: number;
  title: string;
  content: string;
  authorName: string;
  recipientCount: number | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ---------------------------------------------------------------- auth-service

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  avatarUrl: string | null;
  status: "PENDING" | "ACTIVE" | "LOCKED";
  roles: Role[];
  createdAt: string;
  failedLoginsSinceLastSuccess: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  /** Thời hạn access token, tính bằng giây. */
  expiresIn: number;
  user: User;
}

// ---------------------------------------------------------------- course-service

export type CourseLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type CourseStatus = "DRAFT" | "PENDING_REVIEW" | "PUBLISHED" | "ARCHIVED";
export type LessonType = "VIDEO" | "ARTICLE" | "FILE" | "QUIZ";

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  position: number;
  parentId: number | null;
  parentName: string | null;
  createdAt: string;
  updatedAt: string;
  subCategories: Category[];
}

export interface CourseSummary {
  id: number;
  categoryId: number;
  categoryName: string;
  instructorId: number;
  instructorName: string | null;
  title: string;
  slug: string;
  summary: string | null;
  thumbnailUrl: string | null;
  level: CourseLevel;
  price: number;
  status: CourseStatus;
  totalLessons: number;
  totalDurationSeconds: number;
  studentCount: number;
  ratingAvg: number;
  ratingCount: number;
  publishedAt: string | null;
}

export interface Course extends CourseSummary {
  description: string | null;
  language: string;
  createdAt: string;
  updatedAt: string;
}

export interface CourseInput {
  categoryId: number;
  instructorName?: string;
  title: string;
  slug?: string;
  summary?: string;
  description?: string;
  thumbnailUrl?: string;
  level?: CourseLevel;
  language?: string;
  price?: number;
}

export interface LessonResource {
  id: number;
  lessonId: number;
  name: string;
  fileUrl: string;
  createdAt: string;
}

export interface Lesson {
  id: number;
  sectionId: number;
  courseId: number;
  title: string;
  /** null khi người xem chưa có quyền đọc nội dung (chưa ghi danh, bài không phải xem thử). */
  content: string | null;
  contentUrl: string | null;
  type: LessonType;
  durationSeconds: number;
  position: number;
  isPreview: boolean;
  createdAt: string;
  updatedAt: string;
  resources: LessonResource[];
}

export interface LessonInput {
  title: string;
  content?: string;
  contentUrl?: string;
  type?: LessonType;
  durationSeconds?: number;
  position?: number;
  isPreview?: boolean;
}

export interface Section {
  id: number;
  courseId: number;
  title: string;
  position: number;
  createdAt: string;
  updatedAt: string;
  lessons: Lesson[];
}

// ---------------------------------------------------------------- enrollment-service

export type EnrollmentStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";
export type LessonProgressStatus = "IN_PROGRESS" | "COMPLETED";

export interface Enrollment {
  id: number;
  userId: number;
  courseId: number;
  courseTitle: string;
  status: EnrollmentStatus;
  progressPercent: number;
  enrolledAt: string;
  completedAt: string | null;
  lastAccessedAt: string | null;
}

export interface LessonProgress {
  lessonId: number;
  status: LessonProgressStatus;
  watchedSeconds: number;
  completedAt: string | null;
}

export interface CourseProgress {
  courseId: number;
  enrollmentId: number;
  courseTitle: string;
  status: EnrollmentStatus;
  progressPercent: number;
  completedLessonsCount: number;
  totalLessonsCount: number;
  lastAccessedAt: string | null;
  certificateCode: string | null;
  /** Chỉ gồm những bài đã có tiến độ, không phải mọi bài của khóa. */
  lessons: LessonProgress[];
}

export interface Certificate {
  id: number;
  enrollmentId: number;
  userId: number;
  courseId: number;
  courseTitle: string;
  learnerName: string;
  certificateCode: string;
  fileUrl: string;
  issuedAt: string;
}

/** Thông tin công khai; không chứa email hay ID nội bộ. */
export interface CertificateVerification {
  learnerName: string;
  courseTitle: string;
  issuedAt: string;
  certificateCode: string;
}

// ---------------------------------------------------------------- quiz-service

export type QuizStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type QuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE";
export type AttemptStatus = "IN_PROGRESS" | "SUBMITTED" | "EXPIRED";

export interface Quiz {
  id: number;
  courseId: number;
  lessonId: number | null;
  title: string;
  description: string | null;
  timeLimitMinutes: number | null;
  passScore: number;
  maxAttempts: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  questionsPerAttempt: number | null;
  status: QuizStatus;
  createdBy: number;
  totalQuestions: number;
  totalScore: number;
  createdAt: string;
  updatedAt: string;
}

export interface QuizInput {
  courseId?: number;
  lessonId?: number | null;
  title: string;
  description?: string;
  timeLimitMinutes?: number | null;
  passScore?: number;
  maxAttempts?: number;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  questionsPerAttempt?: number | null;
}

export interface AnswerOption {
  id: number;
  content: string;
  /** Không có trong đề cho học viên (GET /take). */
  isCorrect?: boolean;
  position: number;
}

export interface Question {
  id: number;
  content: string;
  type: QuestionType;
  score: number;
  position: number;
  /** Không có trong đề cho học viên. */
  explanation?: string;
  options: AnswerOption[];
}

export interface QuestionInput {
  content: string;
  type: QuestionType;
  score?: number;
  position?: number;
  explanation?: string;
  options: { content: string; isCorrect: boolean; position?: number }[];
}

export interface QuizDetail extends Quiz {
  questions: Question[];
}

export interface QuizAttempt {
  id: number;
  quizId: number;
  quizTitle: string;
  userId: number;
  attemptNo: number;
  status: AttemptStatus;
  score?: number;
  passed?: boolean;
  startedAt: string;
  submittedAt?: string;
  timeLimitMinutes?: number;
  remainingSeconds?: number;
}

export interface QuestionResult {
  questionId: number;
  content: string;
  type: QuestionType;
  questionScore: number;
  earnedScore: number;
  /** Java khai `boolean isCorrect` nên Jackson có thể đặt tên `correct`; đọc qua isQuestionCorrect(). */
  correct?: boolean;
  isCorrect?: boolean;
  explanation: string | null;
  selectedOptionIds: number[];
  correctOptionIds: number[];
  options: AnswerOption[];
}

export interface QuizResult {
  attemptId: number;
  quizId: number;
  quizTitle: string;
  userId: number;
  attemptNo: number;
  /** Điểm theo thang 100. */
  score: number;
  passScore: number;
  passed: boolean;
  startedAt: string;
  submittedAt: string;
  questionResults: QuestionResult[];
}

export function isQuestionCorrect(q: QuestionResult): boolean {
  return Boolean(q.correct ?? q.isCorrect);
}

// ---------------------------------------------------------------- notification-service

export interface Notification {
  id: number;
  type: "ENROLLMENT_SUCCESS" | "COURSE_COMPLETED" | "QUIZ_GRADED" | "CERTIFICATE_ISSUED" | string;
  title: string;
  /** Có thẻ <b>; hiển thị qua safeNotificationHtml(). */
  content: string;
  /** Đường dẫn trên web, mở khi bấm; đi qua notificationHref() chứ không dùng thẳng. */
  linkUrl: string | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

/** GET/PUT /api/notifications/preferences. emailEnabled lưu sẵn, chưa có kênh email. */
export interface NotificationPreference {
  inAppEnabled: boolean;
  emailEnabled: boolean;
}
