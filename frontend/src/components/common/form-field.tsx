import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { FieldError } from "./field-error";

/** Ghi chú nhỏ dưới ô nhập. */
export function FieldHint({ children }: { children: ReactNode }) {
  return <p className="text-xs text-muted-foreground">{children}</p>;
}

/**
 * Một ô trong form: nhãn (dấu * nếu bắt buộc), ô nhập, ghi chú, lỗi của backend ngay dưới ô.
 * Ghi chú ẩn khi có lỗi để người dùng chỉ đọc một dòng.
 */
export function FormField({
  id,
  label,
  required,
  hint,
  error,
  className,
  children,
}: {
  id?: string;
  label: ReactNode;
  required?: boolean;
  hint?: ReactNode;
  error?: string | null;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={id}>
        {label}
        {required && (
          <span className="text-destructive" aria-hidden>
            *
          </span>
        )}
      </Label>
      {children}
      {hint && !error && <FieldHint>{hint}</FieldHint>}
      <FieldError message={error} />
    </div>
  );
}

/**
 * Một nhóm ô trong form dài: tiêu đề + mô tả bên trái (desktop), các ô bên phải trong thẻ trắng.
 * Trên điện thoại tiêu đề nằm trên.
 */
export function FormSection({ title, description, children }: { title: ReactNode; description?: ReactNode; children: ReactNode }) {
  return (
    <section className="grid gap-4 md:grid-cols-[minmax(0,15rem)_minmax(0,1fr)] md:gap-8">
      <div className="space-y-1">
        <h2 className="text-subheading">{title}</h2>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      <Card>
        <div className="space-y-5 px-(--card-spacing)">{children}</div>
      </Card>
    </section>
  );
}

/** Thanh nút cuối form. `sticky`: dính đáy màn hình khi form dài. */
export function FormActions({ sticky, className, children }: { sticky?: boolean; className?: string; children: ReactNode }) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-end gap-2",
        sticky && "sticky bottom-0 z-10 -mx-4 border-t bg-card/90 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6",
        className,
      )}
    >
      {children}
    </div>
  );
}
