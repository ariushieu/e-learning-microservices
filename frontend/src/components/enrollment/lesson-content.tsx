import { DownloadIcon, ExternalLinkIcon, FileDownIcon, InfoIcon, PaperclipIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
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

function ExternalButton({ href, children }: { href: string; children: ReactNode }) {
  return (
    <Button asChild variant="outline" size="lg">
      <a href={href} target="_blank" rel="noopener noreferrer">
        {children} <ExternalLinkIcon />
      </a>
    </Button>
  );
}

function VideoPlayer({ url, title }: { url: string; title: string }) {
  const yt = youtubeId(url);
  if (yt) {
    return (
      <div className="aspect-video w-full overflow-hidden rounded-xl bg-black">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${yt}`}
          title={title}
          className="size-full"
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
  return (
    <div className="flex aspect-video w-full flex-col items-center justify-center gap-4 rounded-xl border bg-muted/40 px-6 text-center">
      <p className="text-sm text-muted-foreground">Video được lưu trên trang ngoài.</p>
      <ExternalButton href={url}>Mở video</ExternalButton>
    </div>
  );
}

function FileCard({ href, title }: { href: string; title: string }) {
  return (
    <div className="flex flex-wrap items-center gap-4 rounded-xl border bg-card p-4">
      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
        <FileDownIcon className="size-5" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium">{title}</p>
        <p className="text-sm text-muted-foreground">Tài liệu của bài học</p>
      </div>
      <Button asChild>
        <a href={href} target="_blank" rel="noopener noreferrer">
          <DownloadIcon /> Tải tài liệu
        </a>
      </Button>
    </div>
  );
}

/** Nội dung một bài học. Dùng được ở server; không render HTML từ giảng viên, chỉ văn bản thuần. */
export function LessonContent({ lesson }: { lesson: Lesson }) {
  const url = safeUrl(lesson.contentUrl);
  const hasContent = Boolean(lesson.content || url || lesson.resources.length);

  if (!hasContent) {
    return lesson.isPreview || lesson.type === "QUIZ" ? null : (
      <Alert>
        <InfoIcon />
        <AlertDescription>
          Nội dung bài này chỉ dành cho học viên đã ghi danh. Nếu bạn đã ghi danh mà vẫn thấy thông báo này, có thể giảng viên
          chưa thêm nội dung hoặc dịch vụ ghi danh đang tạm ngưng — thử tải lại sau.
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      {url && lesson.type === "VIDEO" && <VideoPlayer url={url} title={lesson.title} />}
      {url && lesson.type === "FILE" && <FileCard href={url} title={lesson.title} />}
      {url && (lesson.type === "ARTICLE" || lesson.type === "QUIZ") && (
        <div>
          <ExternalButton href={url}>Mở liên kết</ExternalButton>
        </div>
      )}
      {lesson.content && <div className="lesson-content max-w-3xl text-[15px] text-foreground/90">{lesson.content}</div>}
      {lesson.resources.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-semibold">Tài liệu đính kèm</h3>
          <ul className="grid gap-2 sm:grid-cols-2">
            {lesson.resources.map((r) => {
              const href = safeUrl(r.fileUrl);
              return (
                <li key={r.id} className="flex items-center gap-3 rounded-lg border bg-card px-3 py-2.5 text-sm">
                  <PaperclipIcon className="size-4 shrink-0 text-muted-foreground" />
                  <span className="min-w-0 flex-1 truncate">{r.name}</span>
                  {href && (
                    <a
                      href={href}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="shrink-0 font-medium text-primary underline-offset-4 hover:underline"
                    >
                      Tải về
                    </a>
                  )}
                </li>
              );
            })}
          </ul>
        </section>
      )}
    </div>
  );
}
