import { CirclePlayIcon, DownloadIcon, ExternalLinkIcon, FileDownIcon, InfoIcon, PaperclipIcon } from "lucide-react";
import type { ReactNode } from "react";
import { IconTile } from "@/components/common/icon-tile";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { safeUrl } from "@/lib/safe-url";
import type { Lesson } from "@/lib/types";

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
      <div className="aspect-video w-full overflow-hidden rounded-xl bg-sidebar shadow-card ring-1 ring-border">
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
    return <video key={url} src={url} controls className="aspect-video w-full rounded-xl bg-sidebar shadow-card ring-1 ring-border" />;
  }
  return (
    <div className="flex aspect-video w-full flex-col items-center justify-center gap-4 rounded-xl bg-muted px-6 text-center ring-1 ring-border">
      <IconTile icon={CirclePlayIcon} size="lg" />
      <p className="text-sm text-muted-foreground">Video được lưu trên trang ngoài.</p>
      <ExternalButton href={url}>Mở video</ExternalButton>
    </div>
  );
}

function FileCard({ href, title }: { href: string; title: string }) {
  return (
    <div className="flex flex-wrap items-center gap-4 rounded-xl bg-card p-4 shadow-card ring-1 ring-border">
      <IconTile icon={FileDownIcon} />
      <div className="min-w-0 flex-1">
        <p className="truncate text-subheading">{title}</p>
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
  const url = safeUrl(lesson.contentUrl, { allowInternal: true });
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
      {lesson.content && <div className="lesson-content max-w-prose text-base text-foreground/90">{lesson.content}</div>}
      {lesson.resources.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-subheading">Tài liệu đính kèm</h3>
          <ul className="grid gap-2 sm:grid-cols-2">
            {lesson.resources.map((r) => {
              const href = safeUrl(r.fileUrl);
              return (
                <li key={r.id} className="flex items-center gap-3 rounded-lg bg-card px-3 py-2.5 text-sm ring-1 ring-border">
                  <PaperclipIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
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
