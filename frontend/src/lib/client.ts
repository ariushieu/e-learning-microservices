"use client";

import { ApiError } from "./errors";

/**
 * Gọi API từ Client Component. Đi qua route /api/... của chính Next.js (xem
 * app/api/[...path]/route.ts), cookie đăng nhập tự đi kèm nên ở đây không cầm token.
 *
 * Trả về phần `data` của ApiResponse, lỗi thì ném ApiError.
 */
export async function api<T = unknown>(path: string, init: { method?: string; body?: unknown } = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      method: init.method ?? "GET",
      headers: init.body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
      cache: "no-store",
    });
  } catch {
    throw new ApiError(0, "NETWORK", "Mất kết nối mạng, thử lại sau.");
  }

  if (res.status === 401 && typeof window !== "undefined") {
    // Phiên đăng nhập đã hết và không làm mới được: đưa về trang đăng nhập, quay lại đúng chỗ cũ.
    // Tải lại hẳn trang chứ không router.push: layout phải đọc lại cookie ở server.
    const next = window.location.pathname + window.location.search;
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = `/login?next=${encodeURIComponent(next)}`;
  }
  if (!res.ok) throw await ApiError.from(res);
  if (res.status === 204) return null as T;
  const json = await res.json();
  return json.data as T;
}
