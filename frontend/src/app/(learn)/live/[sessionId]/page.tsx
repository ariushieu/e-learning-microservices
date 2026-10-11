import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { StatusPage } from "@/components/common/status-page";
import { isId } from "@/components/course/queries";
import { LiveLobby } from "@/components/live/live-lobby";
import { Button } from "@/components/ui/button";
import { ApiError, errorMessage } from "@/lib/errors";
import { gateway, getSession } from "@/lib/server/gateway";
import type { LiveSession } from "@/lib/types";

export const metadata: Metadata = { title: "Lớp học trực tuyến" };

export default async function LivePage({ params }: PageProps<"/live/[sessionId]">) {
  const { sessionId } = await params;
  if (!isId(sessionId)) notFound();
  const session = await getSession();
  if (!session) redirect(`/login?next=${encodeURIComponent(`/live/${sessionId}`)}`);

  let live: LiveSession;
  try {
    live = await gateway<LiveSession>(`/api/live-sessions/${sessionId}`);
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 0;
    if (status === 404) notFound();
    return (
      <StatusPage
        code={status === 403 ? "403" : "Lỗi"}
        title={status === 403 ? "Bạn không ở trong lớp này" : "Không tải được buổi học"}
        description={
          status === 403
            ? "Chỉ học viên đã ghi danh khóa học và giảng viên của khóa mới vào được lớp học trực tuyến."
            : errorMessage(e)
        }
        action={
          <Button asChild size="lg">
            <Link href="/my-courses">Về khóa học của tôi</Link>
          </Button>
        }
      />
    );
  }

  return <LiveLobby session={live} me={{ userId: session.userId, fullName: session.fullName }} />;
}
