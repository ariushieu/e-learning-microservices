import type { Category, CourseLevel } from "@/lib/types";

// Phần riêng của form khóa học; hàm xử lý form dùng chung nằm ở @/lib/forms.

export const COURSE_LEVELS: CourseLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

/** Vị trí kế tiếp: lớn hơn mọi vị trí đang có, bắt đầu từ 1. */
export function nextPosition(items: { position: number }[]): number {
  return items.length ? Math.max(...items.map((i) => i.position)) + 1 : 1;
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

// Giữ tên cũ cho các form đang dùng; trang mới import thẳng từ @/components/common.
export { FieldHint, FormField as Field } from "@/components/common/form-field";
export { NativeSelect } from "@/components/common/native-select";
