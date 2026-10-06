"use client";

import { createContext, useContext, type ReactNode } from "react";
import type { Session } from "@/lib/auth-shared";
import type { Role } from "@/lib/types";

const SessionContext = createContext<Session | null>(null);

/** Đưa thông tin người đang đăng nhập (đọc ở server) xuống cho Client Component. */
export function SessionProvider({ session, children }: { session: Session | null; children: ReactNode }) {
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const session = useContext(SessionContext);
  return {
    session,
    loggedIn: session !== null,
    hasRole: (...roles: Role[]) => !!session && roles.some((r) => session.roles.includes(r)),
  };
}
