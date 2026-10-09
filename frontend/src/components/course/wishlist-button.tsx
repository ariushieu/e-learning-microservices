"use client";

import { HeartIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { formErrorMessage } from "@/lib/forms";
import { cn } from "@/lib/utils";

/**
 * Lưu / bỏ lưu khóa học. "icon": nút tròn nằm đè góc ảnh bìa trên thẻ khóa; "button": nút đầy đủ ở
 * trang chi tiết. Đổi trạng thái ngay khi bấm, lỗi thì trả lại.
 */
export function WishlistButton({
  courseId,
  title,
  initialSaved,
  variant = "icon",
}: {
  courseId: number;
  title: string;
  initialSaved: boolean;
  variant?: "icon" | "button";
}) {
  const router = useRouter();
  const [saved, setSaved] = useState(initialSaved);
  const [pending, setPending] = useState(false);
  const [, startTransition] = useTransition();

  async function toggle() {
    if (pending) return;
    const next = !saved;
    setSaved(next);
    setPending(true);
    try {
      await api(`/api/wishlist/${courseId}`, { method: next ? "PUT" : "DELETE" });
      toast.success(next ? "Đã lưu vào danh sách yêu thích" : "Đã bỏ khỏi danh sách yêu thích");
      startTransition(() => router.refresh());
    } catch (cause) {
      setSaved(!next);
      toast.error(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  const label = saved ? `Bỏ lưu khóa ${title}` : `Lưu khóa ${title} vào yêu thích`;
  const heart = <HeartIcon className={cn(saved && "fill-current")} aria-hidden />;

  if (variant === "button") {
    return (
      <Button type="button" variant="outline" size="lg" className={cn("w-full", saved && "text-destructive")} aria-pressed={saved} disabled={pending} onClick={toggle}>
        {heart}
        {saved ? "Đã lưu vào yêu thích" : "Lưu vào yêu thích"}
      </Button>
    );
  }
  return (
    <Button
      type="button"
      variant="secondary"
      size="icon-sm"
      className={cn("absolute top-2.5 right-2.5 z-10 rounded-full bg-card/90 shadow-sm backdrop-blur hover:bg-card", saved && "text-destructive")}
      aria-label={label}
      aria-pressed={saved}
      disabled={pending}
      onClick={toggle}
    >
      {heart}
    </Button>
  );
}
