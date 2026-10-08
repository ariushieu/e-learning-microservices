// Phần xử lý token dùng chung cho proxy.ts, route handler và server component.
// Không import "server-only" ở đây vì proxy.ts cũng dùng file này.

import type { AuthResponse, Role } from "./types";

export const ACCESS_COOKIE = "el_access";
export const REFRESH_COOKIE = "el_refresh";

/** Refresh token của auth-service sống 7 ngày. */
const REFRESH_MAX_AGE = 7 * 24 * 60 * 60;

/** Địa chỉ gateway nhìn từ server Next.js. Trong Docker là http://api-gateway:8080. */
export function gatewayUrl(): string {
  return (process.env.GATEWAY_URL ?? "http://localhost:8080").replace(/\/+$/, "");
}

export interface Session {
  userId: number;
  email: string;
  fullName: string;
  roles: Role[];
}

interface JwtPayload {
  sub: string;
  email?: string;
  fullName?: string;
  roles?: Role[];
  exp?: number;
}

/**
 * Đọc phần payload của JWT để biết ai đang đăng nhập và token còn hạn không.
 *
 * Không kiểm chữ ký: việc đó gateway và từng service đã làm với mọi request. Ở đây chỉ dùng để
 * quyết định hiển thị gì, nên token giả chỉ làm lộ ra giao diện trống — mọi dữ liệu vẫn bị chặn
 * ở backend.
 */
export function decodeJwt(token: string): JwtPayload | null {
  try {
    const part = token.split(".")[1];
    const json = atob(part.replace(/-/g, "+").replace(/_/g, "/"));
    const bytes = Uint8Array.from(json, (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    return null;
  }
}

/** Coi token sắp hết hạn trong 30 giây tới là đã hết, để không gửi đi rồi mới nhận 401. */
export function isExpired(token: string, skewSeconds = 30): boolean {
  const exp = decodeJwt(token)?.exp;
  return !exp || exp * 1000 <= Date.now() + skewSeconds * 1000;
}

export function sessionFromToken(token: string | undefined | null): Session | null {
  if (!token || isExpired(token, 0)) return null;
  const p = decodeJwt(token);
  if (!p?.sub) return null;
  return {
    userId: Number(p.sub),
    email: p.email ?? "",
    fullName: p.fullName ?? p.email ?? "",
    roles: p.roles ?? [],
  };
}

export function hasRole(session: Session | null, ...roles: Role[]): boolean {
  return !!session && roles.some((r) => session.roles.includes(r));
}

/** Đổi refresh token lấy cặp token mới. Refresh token cũ bị thu hồi ngay, phải lưu cặp mới. */
export async function refreshTokens(refreshToken: string, userAgent: string | null): Promise<AuthResponse | null> {
  try {
    const res = await fetch(`${gatewayUrl()}/api/auth/refresh-token`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "User-Agent": userAgent ?? "" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data as AuthResponse;
  } catch {
    return null;
  }
}

interface CookieWriter {
  set(name: string, value: string, options: Record<string, unknown>): unknown;
  delete(name: string): unknown;
}

function cookieOptions(maxAge: number) {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    path: "/",
    // Bật khi chạy sau HTTPS. Chạy Docker trên máy qua http://localhost thì phải để false,
    // nếu không trình duyệt bỏ cookie và không ai đăng nhập được.
    secure: process.env.COOKIE_SECURE === "true",
    maxAge,
  };
}

export function writeAuthCookies(cookies: CookieWriter, auth: AuthResponse) {
  cookies.set(ACCESS_COOKIE, auth.accessToken, cookieOptions(auth.expiresIn));
  cookies.set(REFRESH_COOKIE, auth.refreshToken, cookieOptions(REFRESH_MAX_AGE));
}

export function clearAuthCookies(cookies: CookieWriter) {
  cookies.delete(ACCESS_COOKIE);
  cookies.delete(REFRESH_COOKIE);
}
