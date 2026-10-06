import "server-only";

import { cookies } from "next/headers";
import { ACCESS_COOKIE, gatewayUrl, sessionFromToken, type Session } from "../auth-shared";
import { ApiError } from "../errors";

/**
 * Gọi gateway từ Server Component hoặc Server Action, kèm token của người đang đăng nhập.
 *
 * Token đã được proxy.ts làm mới trước khi trang render (Server Component không ghi được
 * cookie), nên ở đây chỉ việc đọc. Không cache: mọi dữ liệu đều phụ thuộc người xem.
 */
export async function gateway<T>(
  path: string,
  init: { method?: string; body?: unknown; anonymous?: boolean } = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (init.body !== undefined) headers["Content-Type"] = "application/json";
  if (!init.anonymous) {
    const token = (await cookies()).get(ACCESS_COOKIE)?.value;
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let res: Response;
  try {
    res = await fetch(`${gatewayUrl()}${path}`, {
      method: init.method ?? "GET",
      headers,
      body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
      cache: "no-store",
    });
  } catch {
    throw new ApiError(503, "GATEWAY_UNREACHABLE", "Không kết nối được tới máy chủ API.");
  }
  if (!res.ok) throw await ApiError.from(res);
  if (res.status === 204) return null as T;
  const json = await res.json();
  return json.data as T;
}

/** Như gateway() nhưng trả null khi 404, cho những trang cần hiển thị "không tìm thấy". */
export async function gatewayOrNull<T>(path: string): Promise<T | null> {
  try {
    return await gateway<T>(path);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 403)) return null;
    throw e;
  }
}

/** Người đang đăng nhập, đọc từ access token trong cookie. null nếu chưa đăng nhập. */
export async function getSession(): Promise<Session | null> {
  return sessionFromToken((await cookies()).get(ACCESS_COOKIE)?.value);
}
