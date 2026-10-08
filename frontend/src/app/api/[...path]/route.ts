import { NextResponse, type NextRequest } from "next/server";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  clearAuthCookies,
  gatewayUrl,
  isExpired,
  refreshTokens,
  writeAuthCookies,
} from "@/lib/auth-shared";
import type { AuthResponse } from "@/lib/types";

/**
 * Cầu nối cho Client Component: trình duyệt gọi /api/... của chính Next.js, route này gắn token
 * từ cookie httpOnly rồi chuyển tiếp nguyên vẹn sang gateway.
 *
 * Nhờ vậy JavaScript trên trình duyệt không bao giờ chạm được vào token, và trình duyệt chỉ nói
 * chuyện với một origin nên không cần CORS. Đường dẫn giữ nguyên như của gateway:
 * /api/courses/1 ở đây chính là /api/courses/1 ở gateway.
 */

// Đăng nhập, đăng ký, đổi token, đăng xuất đi qua Server Action để ghi cookie. Cho đi qua đây
// thì token sẽ nằm trong body trả về cho JavaScript — đúng thứ cần tránh.
const BLOCKED = new Set(["auth/login", "auth/register", "auth/refresh-token", "auth/logout"]);

// Header của gateway mà màn hình cần đọc (thông báo 429, hạn mức còn lại).
const PASS_HEADERS = ["content-type", "content-disposition", "retry-after", "x-ratelimit-remaining", "x-ratelimit-burst-capacity"];

// Luồng thông báo tức thời (Server-Sent Events): trả về từng đoạn ngay khi gateway gửi, không
// gom cả body như response JSON.
const EVENT_STREAM = "text/event-stream";

async function forward(request: NextRequest, path: string, body: ArrayBuffer | undefined, token?: string) {
  const wantsStream = request.headers.get("accept")?.includes(EVENT_STREAM);
  const headers: Record<string, string> = { Accept: wantsStream ? EVENT_STREAM : "application/json" };
  const contentType = request.headers.get("content-type");
  if (contentType) headers["Content-Type"] = contentType;
  if (token) headers.Authorization = `Bearer ${token}`;
  const clientIp = request.headers.get("x-forwarded-for");
  if (clientIp) headers["X-Forwarded-For"] = clientIp;

  return fetch(`${gatewayUrl()}/api/${path}${request.nextUrl.search}`, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
    // Trình duyệt đóng tab thì cắt luôn kết nối tới gateway, nếu không luồng SSE ở
    // notification-service cứ mở mãi cho một người đã đi.
    signal: request.signal,
  });
}

async function handle(request: NextRequest, ctx: RouteContext<"/api/[...path]">) {
  const path = (await ctx.params).path.map(encodeURIComponent).join("/");
  if (BLOCKED.has(path)) {
    return NextResponse.json(
      { success: false, code: "RESOURCE_NOT_FOUND", message: "Không có đường dẫn này", path: `/api/${path}` },
      { status: 404 },
    );
  }

  // Preserve multipart boundaries and file bytes, also when retrying after token refresh.
  const body = request.method === "GET" || request.method === "HEAD" ? undefined : await request.arrayBuffer();
  let token = request.cookies.get(ACCESS_COOKIE)?.value;
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  let refreshed: AuthResponse | null = null;

  if ((!token || isExpired(token)) && refresh) {
    refreshed = await refreshTokens(refresh);
    if (refreshed) token = refreshed.accessToken;
  }

  let upstream: Response;
  try {
    upstream = await forward(request, path, body, token);
    // Token bị gateway từ chối dù chưa hết hạn theo đồng hồ (ví dụ vừa đổi vai trò, server đổi
    // khóa): thử đổi token một lần rồi gửi lại.
    if (upstream.status === 401 && refresh && !refreshed) {
      refreshed = await refreshTokens(refresh);
      if (refreshed) upstream = await forward(request, path, body, refreshed.accessToken);
    }
  } catch {
    return NextResponse.json(
      { success: false, code: "GATEWAY_UNREACHABLE", message: "Không kết nối được tới máy chủ API." },
      { status: 503 },
    );
  }

  const headers = new Headers();
  for (const name of PASS_HEADERS) {
    const value = upstream.headers.get(name);
    if (value) headers.set(name, value);
  }
  const streaming = upstream.ok && upstream.headers.get("content-type")?.startsWith(EVENT_STREAM);
  if (streaming) {
    // no-transform: không cho nén gzip, vì nén thì phải gom đủ một khối mới gửi, sự kiện đến trễ.
    headers.set("cache-control", "no-cache, no-transform");
  }
  const response = new NextResponse(
    streaming ? upstream.body : upstream.status === 204 ? null : await upstream.arrayBuffer(),
    { status: upstream.status, headers },
  );

  if (refreshed) writeAuthCookies(response.cookies, refreshed);
  else if (upstream.status === 401 && refresh) clearAuthCookies(response.cookies);
  return response;
}

export { handle as GET, handle as POST, handle as PUT, handle as PATCH, handle as DELETE };
