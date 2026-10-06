import type { ReactNode } from "react";
import { Alert } from "@/components/ui";
import type { Lesson } from "@/lib/types";

/** Chỉ cho phép link http(s) hoặc đường dẫn nội bộ: URL do giảng viên nhập, có thể là `javascript:`. */
export function safeUrl(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const url = raw.trim();
  if (url.startsWith("/") && !url.startsWith("//")) return url;
  try {
    const u = new URL(url);
    return u.protocol === "http:" || u.protocol === "https:" ? u.toString() : null;
  } catch {
    return null;
  }
}

export function youtubeId(raw: string): string | null {
  let u: URL;
  try {
    u = new URL(raw);
  } catch {
    return null;
  }
  const host = u.hostname.replace(/^(www|m|music)\./, "");
  let id: string | null = null;
  if (host === "youtu.be") id = u.pathname.split("/")[1] ?? null;
  else if (host === "youtube.com" || host === "youtube-nocookie.com") {
    if (u.pathname === "/watch") id = u.searchParams.get("v");
    else id = u.pathname.match(/^\/(?:embed|shorts|live|v)\/([^/]+)/)?.[1] ?? null;
  }
  return id && /^[\w-]{6,20}$/.test(id) ? id : null;
}

function ExternalLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="inline-flex items-center gap-2 rounded-lg border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-medium text-indigo-700 hover:bg-indigo-100"
    >
      {children} ↗
    </a>
  );
}

function VideoPlayer({ url, title }: { url: string; title: string }) {
  const yt = youtubeId(url);
  if (yt) {
    return (
      <div className="aspect-video overflow-hidden rounded-xl bg-black">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${yt}`}
          title={title}
          className="h-full w-full"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
        />
      </div>
    );
  }
  if (/\.(mp4|webm)([?#].*)?$/i.test(url)) {
    return <video key={url} src={url} controls className="aspect-video w-full rounded-xl bg-black" />;
  }
  return <ExternalLink href={url}>Mở video</ExternalLink>;
}

/** Nội dung một bài học. Dùng được ở server; không render HTML từ giảng viên, chỉ văn bản thuần. */
export function LessonContent({ lesson }: { lesson: Lesson }) {
  const url = safeUrl(lesson.contentUrl);
  const hasContent = Boolean(lesson.content || url || lesson.resources.length);

  if (!hasContent) {
    return lesson.isPreview || lesson.type === "QUIZ" ? null : (
      <Alert kind="info">
        Nội dung bài này chỉ dành cho học viên đã ghi danh. Nếu bạn đã ghi danh mà vẫn thấy thông báo này, có thể giảng viên
        chưa thêm nội dung hoặc dịch vụ ghi danh đang tạm ngưng — thử tải lại sau.
      </Alert>
    );
  }

  return (
    <div className="space-y-5">
      {url && lesson.type === "VIDEO" && <VideoPlayer url={url} title={lesson.title} />}
      {url && lesson.type === "FILE" && <ExternalLink href={url}>Tải tài liệu</ExternalLink>}
      {url && (lesson.type === "ARTICLE" || lesson.type === "QUIZ") && <ExternalLink href={url}>Mở liên kết</ExternalLink>}
      {lesson.content && <div className="lesson-content text-slate-700">{lesson.content}</div>}
      {lesson.resources.length > 0 && (
        <div>
          <h3 className="mb-2 text-sm font-semibold text-slate-900">Tài liệu đính kèm</h3>
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
            {lesson.resources.map((r) => {
              const href = safeUrl(r.fileUrl);
              return (
                <li key={r.id} className="flex items-center justify-between gap-3 px-4 py-2 text-sm">
                  <span className="truncate text-slate-700">{r.name}</span>
                  {href && (
                    <a href={href} target="_blank" rel="noopener noreferrer" className="shrink-0 font-medium text-indigo-600 hover:underline">
                      Tải về
                    </a>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
