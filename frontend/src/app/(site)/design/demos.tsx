"use client";

import { Loader2Icon, SendIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

// Các ví dụ cần bấm được trên trang /design. Không gọi API.

export function LoadingButtonDemo() {
  const [pending, setPending] = useState(false);
  return (
    <Button
      disabled={pending}
      onClick={() => {
        setPending(true);
        setTimeout(() => {
          setPending(false);
          toast.success("Đã lưu thay đổi");
        }, 1200);
      }}
    >
      {pending ? <Loader2Icon className="animate-spin" /> : <SendIcon />}
      {pending ? "Đang lưu..." : "Bấm thử (có trạng thái chờ)"}
    </Button>
  );
}

export function ToastDemo() {
  return (
    <div className="flex flex-wrap gap-2">
      <Button variant="outline" onClick={() => toast.success("Ghi danh thành công")}>
        toast.success
      </Button>
      <Button variant="outline" onClick={() => toast.error("Không thể kết nối máy chủ")}>
        toast.error
      </Button>
      <Button variant="outline" onClick={() => toast.info("Bài kiểm tra còn 5 phút")}>
        toast.info
      </Button>
    </div>
  );
}

export function DialogDemo() {
  return (
    <div className="flex flex-wrap gap-2">
      <Dialog>
        <DialogTrigger asChild>
          <Button variant="outline">Mở Dialog (form ngắn)</Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo bài kiểm tra</DialogTitle>
            <DialogDescription>Đặt tên trước, soạn câu hỏi ở bước sau.</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="demo-quiz">Tên bài kiểm tra</Label>
            <Input id="demo-quiz" placeholder="Kiểm tra chương 1" />
          </div>
          <DialogFooter>
            <Button>Tạo</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <AlertDialog>
        <AlertDialogTrigger asChild>
          <Button variant="destructive">
            <Trash2Icon /> Xóa (luôn hỏi lại)
          </Button>
        </AlertDialogTrigger>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa chương này?</AlertDialogTitle>
            <AlertDialogDescription>Mọi bài học trong chương cũng bị xóa. Không hoàn tác được.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant="destructive">Xóa</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export function ChoiceDemo() {
  return (
    <div className="grid gap-6 sm:grid-cols-3">
      <div className="space-y-3">
        <p className="text-sm font-medium">RadioGroup — một đáp án</p>
        <RadioGroup defaultValue="a">
          {["API Gateway", "Kafka", "Redis"].map((v, i) => (
            <div key={v} className="flex items-center gap-2">
              <RadioGroupItem value={"abc"[i]} id={`demo-r-${i}`} />
              <Label htmlFor={`demo-r-${i}`} className="font-normal">
                {v}
              </Label>
            </div>
          ))}
        </RadioGroup>
      </div>
      <div className="space-y-3">
        <p className="text-sm font-medium">Checkbox — nhiều đáp án</p>
        {["Outbox pattern", "Retry + DLT"].map((v, i) => (
          <div key={v} className="flex items-center gap-2">
            <Checkbox id={`demo-c-${i}`} defaultChecked={i === 0} />
            <Label htmlFor={`demo-c-${i}`} className="font-normal">
              {v}
            </Label>
          </div>
        ))}
      </div>
      <div className="space-y-3">
        <p className="text-sm font-medium">Switch + Select</p>
        <div className="flex items-center gap-2">
          <Switch id="demo-s" defaultChecked />
          <Label htmlFor="demo-s" className="font-normal">
            Xáo trộn câu hỏi
          </Label>
        </div>
        <Select defaultValue="BEGINNER">
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="BEGINNER">Cơ bản</SelectItem>
            <SelectItem value="INTERMEDIATE">Trung cấp</SelectItem>
            <SelectItem value="ADVANCED">Nâng cao</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}

export function TabsDemo() {
  return (
    <Tabs defaultValue="content">
      <TabsList>
        <TabsTrigger value="content">Nội dung</TabsTrigger>
        <TabsTrigger value="info">Thông tin</TabsTrigger>
        <TabsTrigger value="quiz">Bài kiểm tra</TabsTrigger>
      </TabsList>
      <TabsContent value="content" className="pt-3 text-sm text-muted-foreground">
        Tabs dùng khi một trang có nhiều phần ngang hàng (soạn khóa học, lọc khóa học của tôi).
      </TabsContent>
      <TabsContent value="info" className="pt-3 text-sm text-muted-foreground">
        Mỗi tab một nội dung, không đổi URL.
      </TabsContent>
      <TabsContent value="quiz" className="pt-3 text-sm text-muted-foreground">
        Tối đa 5 tab; nhiều hơn thì tách trang.
      </TabsContent>
    </Tabs>
  );
}
