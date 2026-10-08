import "server-only";
import { cache } from "react";
import { gateway } from "@/lib/server/gateway";
import { attempt } from "./queries";
import type { InstructorReviews } from "./review-types";

// Chia sẻ trong một lượt render; không lưu chung giữa các tài khoản.
export const getReviewInboxSummary = cache(() =>
  attempt(gateway<InstructorReviews>("/api/instructor/reviews?replied=false&size=1")),
);
