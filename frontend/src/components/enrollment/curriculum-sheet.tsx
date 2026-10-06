"use client";

import { ListIcon } from "lucide-react";
import { useState, type MouseEvent, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";

/** Mục lục khóa học dạng ngăn kéo cho màn hình nhỏ; tự đóng khi chọn một bài. */
export function CurriculumSheet({ summary, children }: { summary: string; children: ReactNode }) {
  const [open, setOpen] = useState(false);

  function onClick(e: MouseEvent<HTMLDivElement>) {
    if ((e.target as HTMLElement).closest("a")) setOpen(false);
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="outline">
          <ListIcon /> Nội dung khóa học
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-full gap-0 p-0 sm:max-w-sm">
        <SheetHeader className="border-b">
          <SheetTitle>Nội dung khóa học</SheetTitle>
          <SheetDescription>{summary}</SheetDescription>
        </SheetHeader>
        <ScrollArea className="min-h-0 flex-1">
          <div onClick={onClick}>{children}</div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
}
