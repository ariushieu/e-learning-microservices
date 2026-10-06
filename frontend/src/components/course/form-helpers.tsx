import { ChevronDownIcon } from "lucide-react";
import type { ComponentProps, ReactNode } from "react";
import { FieldError } from "@/components/common/field-error";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import type { Category, CourseLevel } from "@/lib/types";

// Phần riêng của form khóa học; hàm xử lý form dùng chung nằm ở @/lib/forms.

export const COURSE_LEVELS: CourseLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

/** Vị trí kế tiếp: lớn hơn mọi vị trí đang có, bắt đầu từ 1. */
export function nextPosition(items: { position: number }[]): number {
  return items.length ? Math.max(...items.map((i) => i.position)) + 1 : 1;
}

/**
 * Thẻ <select> gốc của trình duyệt, trông giống Input. Dùng cho form đọc bằng FormData
 * (và form GET chạy được khi tắt JavaScript), nơi Select của Radix không gửi giá trị.
 */
export function NativeSelect({ className, ...props }: ComponentProps<"select">) {
  return (
    <div className="relative">
      <select
        className={cn(
          "h-8 w-full min-w-0 appearance-none rounded-lg border border-input bg-transparent py-1 pr-8 pl-2.5 text-base transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm",
          className,
        )}
        {...props}
      />
      <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
    </div>
  );
}

const SUB_INDENT = `${String.fromCharCode(160).repeat(4)}↳ `;

/** Danh mục gốc và danh mục con (thụt lề) cho thẻ <select>. */
export function CategoryOptions({ tree }: { tree: Category[] }) {
  return (
    <>
      {tree.flatMap((root) => [
        <option key={root.id} value={root.id}>
          {root.name}
        </option>,
        // Trình duyệt gộp khoảng trắng thường trong <option>, nên thụt lề bằng khoảng trắng không ngắt.
        ...(root.subCategories ?? []).map((sub) => (
          <option key={sub.id} value={sub.id}>
            {SUB_INDENT}
            {sub.name}
          </option>
        )),
      ])}
    </>
  );
}

/** Ghi chú nhỏ dưới ô nhập. */
export function FieldHint({ children }: { children: ReactNode }) {
  return <p className="text-xs text-muted-foreground">{children}</p>;
}

/** Một ô trong form: nhãn, ô nhập, ghi chú và lỗi của backend. */
export function Field({
  id,
  label,
  required,
  hint,
  error,
  children,
}: {
  id?: string;
  label: string;
  required?: boolean;
  hint?: ReactNode;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>
        {label}
        {required && <span className="text-destructive">*</span>}
      </Label>
      {children}
      {hint && <FieldHint>{hint}</FieldHint>}
      <FieldError message={error} />
    </div>
  );
}
