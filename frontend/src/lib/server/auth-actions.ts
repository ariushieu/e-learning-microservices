"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { REFRESH_COOKIE, clearAuthCookies, gatewayUrl, writeAuthCookies } from "../auth-shared";
import { ApiError, errorMessage } from "../errors";
import type { AuthResponse } from "../types";

export interface FormState {
  error?: string;
  fieldErrors?: Record<string, string>;
  values?: Record<string, string>;
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${gatewayUrl()}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  });
  if (!res.ok) throw await ApiError.from(res);
  return (await res.json()).data as T;
}

/** Chỉ cho quay về đường dẫn nội bộ, để link đăng nhập không bị lợi dụng chuyển sang trang lạ. */
function safeNext(next: FormDataEntryValue | null): string {
  const value = typeof next === "string" ? next : "";
  return value.startsWith("/") && !value.startsWith("//") ? value : "/";
}

function toState(e: unknown, values: Record<string, string>): FormState {
  if (e instanceof ApiError && e.fieldErrors.length) {
    return { values, fieldErrors: Object.fromEntries(e.fieldErrors.map((f) => [f.field, f.message])) };
  }
  if (e instanceof ApiError && e.status === 401) return { values, error: "Email hoặc mật khẩu không đúng." };
  return { values, error: errorMessage(e) };
}

export async function loginAction(_prev: FormState, form: FormData): Promise<FormState> {
  const email = String(form.get("email") ?? "").trim();
  const password = String(form.get("password") ?? "");
  try {
    const auth = await post<AuthResponse>("/api/auth/login", { email, password });
    writeAuthCookies(await cookies(), auth);
  } catch (e) {
    return toState(e, { email });
  }
  redirect(safeNext(form.get("next")));
}

export async function registerAction(_prev: FormState, form: FormData): Promise<FormState> {
  const values = {
    fullName: String(form.get("fullName") ?? "").trim(),
    email: String(form.get("email") ?? "").trim(),
    phone: String(form.get("phone") ?? "").trim(),
  };
  const password = String(form.get("password") ?? "");
  if (password !== String(form.get("confirmPassword") ?? "")) {
    return { values, fieldErrors: { confirmPassword: "Mật khẩu nhập lại không khớp" } };
  }
  try {
    await post("/api/auth/register", { ...values, phone: values.phone || undefined, password });
    // Đăng ký không trả token, nên đăng nhập luôn để người dùng không phải gõ lại.
    const auth = await post<AuthResponse>("/api/auth/login", { email: values.email, password });
    writeAuthCookies(await cookies(), auth);
  } catch (e) {
    if (e instanceof ApiError && e.status === 409) return { values, fieldErrors: { email: "Email này đã được đăng ký" } };
    return toState(e, values);
  }
  redirect(safeNext(form.get("next")));
}

export async function logoutAction(): Promise<void> {
  await endSession("/");
}

export async function logoutToLoginAction(): Promise<void> {
  await endSession("/login");
}

async function endSession(destination: "/" | "/login"): Promise<void> {
  const store = await cookies();
  const refreshToken = store.get(REFRESH_COOKIE)?.value;
  if (refreshToken) {
    // Thu hồi refresh token ở auth-service; lỗi mạng cũng vẫn xóa cookie như thường.
    await post("/api/auth/logout", { refreshToken }).catch(() => undefined);
  }
  clearAuthCookies(store);
  redirect(destination);
}
