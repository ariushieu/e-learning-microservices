"use client";

import { CheckCheckIcon, Loader2Icon, Settings2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Switch } from "@/components/ui/switch";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { NotificationPreference } from "@/lib/types";

/** Lưu tùy chọn. PUT ghi đè cả bộ nên luôn gửi lại emailEnabled đang có. */
function savePreferences(next: NotificationPreference) {
  return api<NotificationPreference>("/api/notifications/preferences", { method: "PUT", body: next });
}

/** Nút trên đầu trang Thông báo: đọc tất cả, cài đặt nhận thông báo. */
export function NotificationActions({ unread, preferences }: { unread: number; preferences: NotificationPreference | null }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  async function readAll() {
    setBusy(true);
    try {
      await api("/api/notifications/read", { method: "PATCH" });
      router.refresh();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      {unread > 0 && (
        <Button variant="outline" onClick={readAll} disabled={busy}>
          {busy ? <Loader2Icon className="animate-spin" /> : <CheckCheckIcon />}
          Đánh dấu đã đọc tất cả
        </Button>
      )}
      {preferences && <NotificationSettings preferences={preferences} />}
    </div>
  );
}

function NotificationSettings({ preferences }: { preferences: NotificationPreference }) {
  const router = useRouter();
  const [inApp, setInApp] = useState(preferences.inAppEnabled);
  const [saving, setSaving] = useState(false);
  // Bật lại từ Callout thì server trả giá trị mới: công tắc theo luôn, không cần dựng lại cả
  // popover (dựng lại thì popover đang mở bị đóng).
  const [synced, setSynced] = useState(preferences.inAppEnabled);
  if (preferences.inAppEnabled !== synced) {
    setSynced(preferences.inAppEnabled);
    setInApp(preferences.inAppEnabled);
  }

  async function toggle(next: boolean) {
    setInApp(next);
    setSaving(true);
    try {
      await savePreferences({ ...preferences, inAppEnabled: next });
      toast.success("Đã lưu cài đặt thông báo");
      router.refresh();
    } catch (e) {
      setInApp(!next);
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Cài đặt thông báo">
          <Settings2Icon />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(20rem,calc(100vw-1rem))]">
        <p className="text-subheading">Cài đặt thông báo</p>
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <Label htmlFor="notify-in-app">Thông báo trong ứng dụng</Label>
            <p className="mt-1 text-caption text-muted-foreground">
              Ghi danh, hoàn thành khóa, kết quả bài kiểm tra và chứng chỉ. Tắt thì các sự kiện mới không tạo thông báo; thông báo cũ vẫn còn.
            </p>
          </div>
          <Switch id="notify-in-app" checked={inApp} onCheckedChange={toggle} disabled={saving} />
        </div>
      </PopoverContent>
    </Popover>
  );
}

/** Nút trong Callout khi đang tắt thông báo: bật lại ngay, không phải mở cài đặt. */
export function EnableInAppButton({ preferences }: { preferences: NotificationPreference }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  async function enable() {
    setBusy(true);
    try {
      await savePreferences({ ...preferences, inAppEnabled: true });
      toast.success("Đã bật lại thông báo");
      router.refresh();
    } catch (e) {
      toast.error(errorMessage(e));
      setBusy(false);
    }
  }

  return (
    <Button onClick={enable} disabled={busy}>
      {busy && <Loader2Icon className="animate-spin" />}
      Bật lại
    </Button>
  );
}
