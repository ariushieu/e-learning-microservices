"use client";

import { DownloadIcon, Loader2Icon, UploadIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

type RowError = { line: number; message: string };

export function ImportQuestionsDialog({ quizId, onImported }: { quizId: number; onImported: () => void }) {
  const router = useRouter();
  const id = useId();
  const [open, setOpen] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [rows, setRows] = useState<RowError[]>([]);

  function changeOpen(value: boolean) {
    if (busy) return;
    setOpen(value);
    setFile(null);
    setError(null);
    setRows([]);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setRows([]);
    if (!file || file.size === 0) { setError("Chọn file CSV có câu hỏi."); return; }
    if (file.size > 1024 * 1024) { setError("File tối đa 1 MB (1048576 byte)."); return; }
    setError(null);
    setBusy(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(`/api/quizzes/${quizId}/questions/import`, { method: "POST", body });
      const result = await response.json();
      if (!response.ok) {
        setError(result.message ?? "Không nhập được file. Vui lòng thử lại.");
        setRows(Array.isArray(result.errors) ? result.errors : []);
        return;
      }
      toast.success(`Đã nhập ${result.data.imported} câu hỏi`);
      setOpen(false);
      setFile(null);
      onImported();
      router.refresh();
    } catch {
      setError("Không nhận được kết quả nhập. Kiểm tra lại danh sách câu hỏi trước khi thử lại để tránh nhập trùng.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogTrigger asChild>
        <Button variant="outline"><UploadIcon /> Nhập từ CSV</Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-xl" showCloseButton={!busy}>
        <DialogHeader>
          <DialogTitle>Nhập câu hỏi từ CSV</DialogTitle>
          <DialogDescription>
            Thêm vào cuối đề, giữ nguyên câu hỏi đang có. Một dòng sai thì không nhập câu nào.
          </DialogDescription>
        </DialogHeader>
        <Button asChild variant="outline" className="w-fit">
          <a href={`/api/quizzes/${quizId}/questions/import/template`}><DownloadIcon /> Tải file mẫu</a>
        </Button>
        <p className="text-sm text-muted-foreground">
          Mỗi dòng một câu, đánh dấu * trước đáp án đúng. Tối đa 200 câu và 1 MB.
          Khi lưu trong Excel, chọn CSV UTF-8. Số dòng lỗi tính cả dòng tiêu đề và các dòng xuống hàng trong ô.
        </p>
        <form onSubmit={submit} className="min-w-0 space-y-4" aria-busy={busy}>
          <FormField id={id} label="File câu hỏi CSV" required hint="Dùng cấu trúc cột của file mẫu; điểm dùng dấu chấm thập phân.">
            <Input id={id} type="file" accept=".csv,text/csv" disabled={busy}
              onChange={(event) => { setFile(event.target.files?.[0] ?? null); setError(null); setRows([]); }} />
          </FormField>
          {error && <ErrorAlert message={error} />}
          {rows.length > 0 && (
            <div className="max-h-64 overflow-auto rounded-lg border" aria-label="Lỗi trong file CSV">
              <Table className="table-fixed">
                <TableHeader>
                  <TableRow><TableHead className="w-16">Dòng</TableHead><TableHead>Lỗi cần sửa</TableHead></TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((row, index) => (
                    <TableRow key={index}>
                      <TableCell className="tabular-nums">{row.line}</TableCell>
                      <TableCell className="whitespace-normal break-words">{row.message}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" disabled={busy} onClick={() => changeOpen(false)}>Hủy</Button>
            <Button type="submit" disabled={busy || !file}>
              {busy ? <Loader2Icon className="animate-spin" /> : <UploadIcon />}
              {busy ? "Đang nhập…" : "Nhập câu hỏi"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
