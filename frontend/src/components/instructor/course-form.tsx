"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import type { Category, Course, CourseInput, CourseLevel } from "@/lib/types";
import { Alert, Button, Field, Input, Select, Textarea } from "@/components/ui";
import {
  CategoryOptions,
  FieldError,
  fieldErrorMap,
  formErrorMessage,
  levelLabels,
  optional,
  optionalNumber,
  text,
} from "./form-helpers";

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
  const [saved, setSaved] = useState(false);

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
    setSaved(false);
    try {
      if (course) {
        await api<Course>(`/api/courses/${course.id}`, { method: "PUT", body });
        setSaved(true);
        router.refresh();
      } else {
        const created = await api<Course>("/api/courses", { method: "POST", body });
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
    <form onSubmit={onSubmit} className="space-y-5">
      {error && <Alert>{error}</Alert>}
      {saved && <Alert kind="success">Đã lưu thông tin khóa học.</Alert>}

      <fieldset disabled={disabled || pending} className="space-y-5">
        <div>
          <Field label="Danh mục *">
            <Select name="categoryId" required defaultValue={course?.categoryId ?? ""}>
              <option value="" disabled>
                -- Chọn danh mục --
              </option>
              <CategoryOptions tree={categories} />
            </Select>
          </Field>
          <FieldError message={errors.categoryId} />
        </div>

        <div>
          <Field label="Tên khóa học *">
            <Input name="title" required maxLength={200} defaultValue={course?.title} placeholder="VD: Lập trình Java cơ bản" />
          </Field>
          <FieldError message={errors.title} />
        </div>

        <div>
          <Field label="Slug (đường dẫn)" hint="Để trống thì hệ thống tự sinh từ tên khóa học.">
            <Input name="slug" maxLength={220} defaultValue={course?.slug} placeholder="lap-trinh-java-co-ban" />
          </Field>
          <FieldError message={errors.slug} />
        </div>

        <div>
          <Field label="Tóm tắt" hint="Một hai câu giới thiệu, hiện trên thẻ khóa học. Tối đa 500 ký tự.">
            <Textarea name="summary" rows={2} maxLength={500} defaultValue={course?.summary ?? ""} />
          </Field>
          <FieldError message={errors.summary} />
        </div>

        <div>
          <Field label="Mô tả chi tiết">
            <Textarea name="description" rows={6} defaultValue={course?.description ?? ""} />
          </Field>
          <FieldError message={errors.description} />
        </div>

        <div>
          <Field label="Ảnh đại diện (URL)">
            <Input
              name="thumbnailUrl"
              type="url"
              maxLength={500}
              defaultValue={course?.thumbnailUrl ?? ""}
              placeholder="https://..."
            />
          </Field>
          <FieldError message={errors.thumbnailUrl} />
        </div>

        <div className="grid gap-5 sm:grid-cols-3">
          <div>
            <Field label="Trình độ">
              <Select name="level" defaultValue={course?.level ?? "BEGINNER"}>
                {(Object.keys(levelLabels) as CourseLevel[]).map((l) => (
                  <option key={l} value={l}>
                    {levelLabels[l]}
                  </option>
                ))}
              </Select>
            </Field>
            <FieldError message={errors.level} />
          </div>
          <div>
            <Field label="Ngôn ngữ" hint="Mã ngôn ngữ, VD: vi, en">
              <Input name="language" maxLength={10} defaultValue={course?.language ?? "vi"} />
            </Field>
            <FieldError message={errors.language} />
          </div>
          <div>
            <Field label="Học phí (VNĐ)" hint="0 = miễn phí">
              <Input name="price" type="number" min={0} step="any" defaultValue={course?.price ?? 0} />
            </Field>
            <FieldError message={errors.price} />
          </div>
        </div>
      </fieldset>

      <div className="flex justify-end">
        <Button type="submit" loading={pending} disabled={disabled}>
          {course ? "Lưu thay đổi" : "Tạo khóa học"}
        </Button>
      </div>
    </form>
  );
}
