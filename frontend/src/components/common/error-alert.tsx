import { CircleAlertIcon } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

/** Lỗi trả về từ API, hiển thị trong trang hoặc trong form. */
export function ErrorAlert({ title, message }: { title?: string; message: string }) {
  return (
    <Alert variant="destructive">
      <CircleAlertIcon />
      {title && <AlertTitle>{title}</AlertTitle>}
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
}
