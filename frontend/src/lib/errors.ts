import type { ErrorBody } from "./types";

/**
 * Lỗi từ backend, giữ nguyên `code` của ErrorResponse để màn hình xử lý theo mã
 * (RESOURCE_NOT_FOUND, FORBIDDEN...) chứ không so chuỗi thông báo.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fieldErrors: { field: string; message: string }[] = [],
  ) {
    super(message);
    this.name = "ApiError";
  }

  static async from(res: Response): Promise<ApiError> {
    let body: Partial<ErrorBody> | null = null;
    try {
      body = await res.json();
    } catch {
      body = null;
    }
    return new ApiError(
      res.status,
      body?.code ?? `HTTP_${res.status}`,
      body?.message ?? defaultMessage(res.status),
      body?.fieldErrors ?? [],
    );
  }
}

export function defaultMessage(status: number): string {
  switch (status) {
    case 401:
      return "Phiên đăng nhập đã hết, vui lòng đăng nhập lại.";
    case 403:
      return "Bạn không có quyền thực hiện thao tác này.";
    case 404:
      return "Không tìm thấy dữ liệu.";
    case 429:
      return "Bạn thao tác quá nhanh, thử lại sau ít giây.";
    case 502:
    case 503:
    case 504:
      return "Một dịch vụ đang tạm ngưng, thử lại sau.";
    default:
      return "Có lỗi xảy ra, thử lại sau.";
  }
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.fieldErrors.length) return e.fieldErrors.map((f) => f.message).join(". ");
    return e.message;
  }
  if (e instanceof Error) return e.message;
  return "Có lỗi xảy ra";
}
