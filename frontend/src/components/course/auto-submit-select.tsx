"use client";

import type { ComponentProps } from "react";
import { NativeSelect } from "@/components/common/native-select";

/** Ô chọn gửi form ngay khi đổi giá trị, để bộ lọc không cần thêm nút "Lọc". */
export function AutoSubmitSelect(props: ComponentProps<typeof NativeSelect>) {
  return <NativeSelect {...props} onChange={(event) => event.currentTarget.form?.requestSubmit()} />;
}
