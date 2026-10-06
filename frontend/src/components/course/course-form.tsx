"use client";

import { Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { label } from "@/lib/format";
import { fieldErrorMap, formErrorMessage, optional, optionalNumber, text } from "@/lib/forms";
import type { Category, Course, CourseInput, CourseLevel } from "@/lib/types";
import { CategoryOptions, COURSE_LEVELS, Field, NativeSelect } from "./form-helpers";

/**
 * Form tạo khóa học (không có `course`) hoặc sửa thông tin (có `course`).
 * PUT ghi đè mọi trường nên lúc sửa luôn gửi đủ cả object, kể cả slug cũ để slug không bị sinh lại.
 */
export function CourseForm({
  categories,
  course,
  instructorName,
  disabled,
}: {
  categories: Category[];
  course?: Course;
  instructorName: string;
  disabled?: boolean;
}) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const body: CourseInput = {
      categoryId: Number(fd.get("categoryId")),
      instructorName: course?.instructorName || instructorName || undefined,
      title: text(fd.get("title")),
      slug: optional(fd.get("slug")),
      summary: optional(fd.get("summary")),
      description: optional(fd.get("description")),
      thumbnailUrl: optional(fd.get("thumbnailUrl")),
      level: (text(fd.get("level")) || "BEGINNER") as CourseLevel,
      language: optional(fd.get("language")) ?? "vi",
      price: optionalNumber(fd.get("price")) ?? 0,
    };

    setPending(true);
    setError(null);
    setErrors({});
    try {
      if (course) {
        await api<Course>(`/api/courses/${course.id}`, { method: "PUT", body });
        toast.success("Đã lưu thông tin khóa học");
        router.refresh();
      } else {
        const created = await api<Course>("/api/courses", { method: "POST", body });
        toast.success("Đã tạo khóa học");
        router.push(`/instructor/courses/${created.id}`);
      }
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      {error && <ErrorAlert message={error} />}

      <fieldset disabled={disabled || pending} className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <CardHeader>
            <CardTitle>Thông tin chính</CardTitle>
            <CardDescription>Tên, mô tả và ảnh bìa hiển thị trên trang khóa học.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <Field id="title" label="Tên khóa học" required error={errors.title}>
              <Input id="title" name="title" required maxLength={200} defaultValue={course?.title} placeholder="VD: Lập trình Java cơ bản" />
            </Field>
            <Field id="slug" label="Slug (đường dẫn)" error={errors.slug} hint="Để trống thì hệ thống tự sinh từ tên khóa học.">
              <Input id="slug" name="slug" maxLength={220} defaultValue={course?.slug} placeholder="lap-trinh-java-co-ban" />
            </Field>
            <Field id="summary" label="Tóm tắt" error={errors.summary} hint="Một hai câu giới thiệu, hiện trên thẻ khóa học. Tối đa 500 ký tự.">
              <Textarea id="summary" name="summary" rows={2} maxLength={500} defaultValue={course?.summary ?? ""} />
            </Field>
            <Field id="description" label="Mô tả chi tiết" error={errors.description}>
              <Textarea id="description" name="description" rows={8} className="min-h-40" defaultValue={course?.description ?? ""} />
            </Field>
            <Field id="thumbnailUrl" label="Ảnh bìa (URL)" error={errors.thumbnailUrl} hint="Ảnh tỉ lệ 16:9 hiển thị đẹp nhất.">
              <Input
                id="thumbnailUrl"
                name="thumbnailUrl"
                type="url"
                maxLength={500}
                defaultValue={course?.thumbnailUrl ?? ""}
                placeholder="https://..."
              />
            </Field>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Phân loại &amp; học phí</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <Field id="categoryId" label="Danh mục" required error={errors.categoryId}>
              <NativeSelect id="categoryId" name="categoryId" required defaultValue={course?.categoryId ?? ""}>
                <option value="" disabled>
                  Chọn danh mục
                </option>
                <CategoryOptions tree={categories} />
              </NativeSelect>
            </Field>
            <Field id="level" label="Trình độ" error={errors.level}>
              <NativeSelect id="level" name="level" defaultValue={course?.level ?? "BEGINNER"}>
                {COURSE_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {label(l)}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field id="language" label="Ngôn ngữ" error={errors.language} hint="Mã ngôn ngữ, VD: vi, en">
              <Input id="language" name="language" maxLength={10} defaultValue={course?.language ?? "vi"} />
            </Field>
            <Field id="price" label="Học phí (VNĐ)" error={errors.price} hint="Nhập 0 nếu miễn phí.">
              <Input id="price" name="price" type="number" min={0} step="any" defaultValue={course?.price ?? 0} />
            </Field>
          </CardContent>
        </Card>
      </fieldset>

      <div className="flex justify-end">
        <Button type="submit" size="lg" disabled={disabled || pending}>
          {pending && <Loader2Icon className="animate-spin" />}
          {course ? "Lưu thay đổi" : "Tạo khóa học"}
        </Button>
      </div>
    </form>
  );
}
