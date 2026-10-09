"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { ComponentProps } from "react";
import { Tabs } from "@/components/ui/tabs";

/**
 * Tabs ghi tab đang mở vào `?tab=` trên URL: tải lại trang, gửi link hay bấm Back vẫn về đúng tab.
 * Giá trị lạ trên URL thì về `defaultValue`.
 */
export function UrlTabs({
  values,
  defaultValue,
  param = "tab",
  ...props
}: Omit<ComponentProps<typeof Tabs>, "value" | "onValueChange"> & { values: string[]; defaultValue: string; param?: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const fromUrl = searchParams.get(param);
  const value = fromUrl && values.includes(fromUrl) ? fromUrl : defaultValue;

  return (
    <Tabs
      {...props}
      value={value}
      onValueChange={(next) => {
        const query = new URLSearchParams(searchParams.toString());
        if (next === defaultValue) query.delete(param);
        else query.set(param, next);
        const s = query.toString();
        router.replace(s ? `${pathname}?${s}` : pathname, { scroll: false });
      }}
    />
  );
}
