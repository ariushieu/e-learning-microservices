import { NextResponse, type NextRequest } from "next/server";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  clearAuthCookies,
  hasRole,
  isExpired,
  refreshTokens,
  sessionFromToken,
  writeAuthCookies,
  type Session,
} from "./lib/auth-shared";
import type { AuthResponse, Role } from "./lib/types";

/**
 * Chạy trước mọi trang, làm hai việc:
 *
 * 1. **Làm mới token.** Access token sống 15 phút. Hết hạn mà còn refresh token thì đổi cặp mới
 *    ngay tại đây, vì Server Component không ghi được cookie — để nó tự đổi thì cặp mới mất,
 *    và refresh token cũ đã bị thu hồi nên lần sau người dùng bị đăng xuất.
 * 2. **Chặn trang theo đăng nhập và vai trò**, để người chưa đăng nhập không thấy khung trang
 *    rỗng rồi mới nhận lỗi. Backend vẫn kiểm lại mọi request; đây chỉ là lớp ngoài cho dễ dùng.
 */
export async function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;

  let session: Session | null = access && !isExpired(access) ? sessionFromToken(access) : null;
  let refreshed: AuthResponse | null = null;
  let refreshFailed = false;

  if (!session && refresh) {
    refreshed = await refreshTokens(refresh, request.headers.get("user-agent"));
    if (refreshed) {
      session = sessionFromToken(refreshed.accessToken);
      // Ghi cookie mới vào chính request đang xử lý, để Server Component của lần render này
      // đọc được token mới chứ không phải token vừa hết hạn.
      request.cookies.set(ACCESS_COOKIE, refreshed.accessToken);
      request.cookies.set(REFRESH_COOKIE, refreshed.refreshToken);
    } else {
      refreshFailed = true;
    }
  }

  let response: NextResponse;
  const required = requiredAccess(pathname);

  if (required && !session) {
    const login = new URL("/login", request.url);
    login.searchParams.set("next", pathname + search);
    response = NextResponse.redirect(login);
  } else if (required && required !== "auth" && !hasRole(session, ...required)) {
    response = NextResponse.redirect(new URL("/khong-co-quyen", request.url));
  } else if (session && (pathname === "/login" || pathname === "/register")) {
    response = NextResponse.redirect(new URL("/", request.url));
  } else {
    response = NextResponse.next({ request: { headers: request.headers } });
  }

  if (refreshed) writeAuthCookies(response.cookies, refreshed);
  else if (refreshFailed) clearAuthCookies(response.cookies);
  return response;
}

/** "auth": chỉ cần đăng nhập; mảng vai trò: cần một trong các vai trò đó; null: công khai. */
function requiredAccess(pathname: string): "auth" | Role[] | null {
  if (pathname.startsWith("/admin")) return ["ROLE_ADMIN"];
  // /instructors/{id} là hồ sơ công khai; chỉ /instructor và các trang con cần quyền.
  if (pathname === "/instructor" || pathname.startsWith("/instructor/")) return ["ROLE_INSTRUCTOR", "ROLE_ADMIN"];
  const privatePrefixes = ["/learn", "/my-courses", "/quizzes", "/attempts", "/notifications", "/certificates", "/profile", "/leaderboard"];
  return privatePrefixes.some((p) => pathname === p || pathname.startsWith(`${p}/`)) ? "auth" : null;
}

export const config = {
  // Bỏ qua /api (route handler tự lo token), file tĩnh và ảnh.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\.[a-zA-Z0-9]+$).*)"],
};
