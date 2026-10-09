"use client";

import { Loader2Icon, MessageCircleQuestionIcon, ReplyIcon, SendIcon, Trash2Icon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { StatusBadge } from "@/components/common/status-badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { formatDate, initials } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { LessonQuestion } from "@/lib/types";
import { cn } from "@/lib/utils";

const MAX = 2000;

const ROLE_LABEL: Record<string, string> = { INSTRUCTOR: "Giảng viên", ADMIN: "Quản trị viên" };

/** Hỏi đáp dưới một bài học: ô đặt câu hỏi rồi danh sách câu hỏi, mới hoạt động nhất ở trên. */
export function LessonQuestions({
  lessonId,
  questions,
  total,
  error,
}: {
  lessonId: number;
  questions: LessonQuestion[];
  total: number;
  error: string | null;
}) {
  return (
    <section id="hoi-dap" className="scroll-mt-24 space-y-4" aria-labelledby="hoi-dap-title">
      <div className="space-y-1">
        <h3 id="hoi-dap-title" className="text-subheading">
          Hỏi đáp về bài học {total > 0 && <span className="text-muted-foreground tabular-nums">({total})</span>}
        </h3>
        <p className="text-sm text-muted-foreground">Chưa hiểu chỗ nào thì hỏi ở đây. Giảng viên và các bạn cùng khóa sẽ trả lời.</p>
      </div>
      <TextComposer
        endpoint={`/api/lessons/${lessonId}/questions`}
        label="Câu hỏi của bạn"
        placeholder="Ví dụ: Vì sao vòng lặp này chạy thiếu một lần?"
        submitLabel="Gửi câu hỏi"
        success="Đã gửi câu hỏi. Giảng viên sẽ nhận được thông báo."
      />
      {error !== null ? (
        <ErrorAlert title="Không tải được hỏi đáp" message={error} />
      ) : questions.length === 0 ? (
        <EmptyState icon={MessageCircleQuestionIcon} title="Chưa có câu hỏi nào" description="Hãy là người đầu tiên đặt câu hỏi cho bài học này." />
      ) : (
        <ol className="space-y-3">
          {questions.map((q) => (
            <li key={q.id}>
              <QuestionCard question={q} />
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

/** Một câu hỏi kèm các câu trả lời. showContext: hiện tên bài/khóa và link tới bài (hộp câu hỏi của giảng viên). */
export function QuestionCard({ question: q, showContext = false }: { question: LessonQuestion; showContext?: boolean }) {
  const [replying, setReplying] = useState(false);
  const base = `/api/lessons/${q.lessonId}/questions/${q.id}`;
  return (
    <Card>
      <CardContent className="space-y-4">
        {showContext && q.lessonTitle && (
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-caption text-muted-foreground">
            <span>
              {q.courseTitle} · <span className="font-medium text-foreground">{q.lessonTitle}</span>
            </span>
            {q.instructorAnswered ? (
              <StatusBadge status="PUBLISHED">Giảng viên đã trả lời</StatusBadge>
            ) : (
              <StatusBadge status="PENDING_REVIEW">Chờ giảng viên trả lời</StatusBadge>
            )}
          </div>
        )}
        <Post
          name={q.authorName}
          role={null}
          content={q.content}
          createdAt={q.createdAt}
          mine={q.mine}
          remove={q.deletable ? { endpoint: base, what: "câu hỏi" } : null}
        />
        {q.answers.length > 0 && (
          <ol className="space-y-3 border-l-2 border-muted pl-4 sm:ml-5">
            {q.answers.map((a) => (
              <li key={a.id}>
                <Post
                  name={a.authorName}
                  role={ROLE_LABEL[a.authorRole] ?? null}
                  content={a.content}
                  createdAt={a.createdAt}
                  mine={a.mine}
                  remove={a.deletable ? { endpoint: `${base}/answers/${a.id}`, what: "câu trả lời" } : null}
                />
              </li>
            ))}
          </ol>
        )}
        {replying ? (
          <div className="sm:ml-5">
            <TextComposer
              endpoint={`${base}/answers`}
              label="Câu trả lời"
              placeholder="Viết câu trả lời…"
              submitLabel="Gửi trả lời"
              success="Đã gửi câu trả lời"
              compact
              onDone={() => setReplying(false)}
              onCancel={() => setReplying(false)}
            />
          </div>
        ) : (
          <div className="flex flex-wrap gap-2 sm:ml-5">
            <Button variant="ghost" size="sm" onClick={() => setReplying(true)}>
              <ReplyIcon aria-hidden /> Trả lời
            </Button>
            {showContext && (
              <Button asChild variant="ghost" size="sm">
                <Link href={`/courses/${q.courseId}`}>Xem trang khóa</Link>
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function Post({
  name,
  role,
  content,
  createdAt,
  mine,
  remove,
}: {
  name: string;
  role: string | null;
  content: string;
  createdAt: string;
  mine: boolean;
  remove: { endpoint: string; what: string } | null;
}) {
  return (
    <div className="flex gap-3">
      <Avatar className="size-9 shrink-0">
        <AvatarFallback className={cn("text-xs", role ? "bg-primary-soft text-primary-strong" : "bg-muted")}>{initials(name)}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-sm font-semibold">{name}</span>
          {role && <Badge variant="secondary">{role}</Badge>}
          {mine && <span className="text-caption text-muted-foreground">(bạn)</span>}
          <time dateTime={createdAt} className="text-caption text-muted-foreground">
            {formatDate(createdAt)}
          </time>
          {remove && <DeleteButton endpoint={remove.endpoint} what={remove.what} />}
        </div>
        <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{content}</p>
      </div>
    </div>
  );
}

function TextComposer({
  endpoint,
  label,
  placeholder,
  submitLabel,
  success,
  compact = false,
  onDone,
  onCancel,
}: {
  endpoint: string;
  label: string;
  placeholder: string;
  submitLabel: string;
  success: string;
  compact?: boolean;
  onDone?: () => void;
  onCancel?: () => void;
}) {
  const router = useRouter();
  const [content, setContent] = useState("");
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const busy = pending || refreshing;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    if (!content.trim()) {
      setError("Nhập nội dung trước khi gửi.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      await api(endpoint, { method: "POST", body: { content } });
      toast.success(success);
      setContent("");
      onDone?.();
      startTransition(() => router.refresh());
    } catch (cause) {
      setError(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-2" aria-label={label}>
      <Textarea
        aria-label={label}
        name="content"
        className={cn("field-sizing-fixed max-h-64", compact ? "min-h-20" : "min-h-24")}
        rows={compact ? 2 : 3}
        maxLength={MAX}
        placeholder={placeholder}
        value={content}
        disabled={busy}
        aria-invalid={Boolean(error)}
        onChange={(e) => {
          setContent(e.target.value);
          setError(null);
        }}
      />
      {error && <ErrorAlert message={error} />}
      <div className="flex flex-wrap items-center gap-2">
        <Button type="submit" size={compact ? "sm" : "default"} disabled={busy}>
          {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : <SendIcon aria-hidden />}
          {submitLabel}
        </Button>
        {onCancel && (
          <Button type="button" variant="ghost" size="sm" disabled={busy} onClick={onCancel}>
            Hủy
          </Button>
        )}
        <span className="ml-auto text-caption text-muted-foreground tabular-nums">
          {content.length}/{MAX}
        </span>
      </div>
    </form>
  );
}

function DeleteButton({ endpoint, what }: { endpoint: string; what: string }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();

  async function remove() {
    setPending(true);
    try {
      await api(endpoint, { method: "DELETE" });
      toast.success(`Đã xóa ${what}`);
      startTransition(() => router.refresh());
    } catch (cause) {
      toast.error(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="ghost" size="icon-sm" className="ml-auto" disabled={pending || refreshing} aria-label={`Xóa ${what}`}>
          {pending || refreshing ? <Loader2Icon className="animate-spin" aria-hidden /> : <Trash2Icon aria-hidden />}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Xóa {what} này?</AlertDialogTitle>
          <AlertDialogDescription>
            {what === "câu hỏi" ? "Câu hỏi và mọi câu trả lời bên dưới sẽ bị xóa." : "Câu trả lời sẽ bị xóa khỏi cuộc hỏi đáp."}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Hủy</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={remove}>
            Xóa
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
