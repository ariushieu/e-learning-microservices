"use client";

import { SearchCheckIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

/** Ô nhập mã chứng chỉ, mở trang xác minh công khai /verify/{mã}. */
export function VerifyCertificateForm() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = code.trim().toUpperCase();
    if (!/^CERT-[A-Z0-9-]{4,}$/.test(value)) {
      setError("Mã chứng chỉ có dạng CERT-…, in ở cuối chứng chỉ.");
      return;
    }
    router.push(`/verify/${encodeURIComponent(value)}`);
  }

  return (
    <form onSubmit={submit} className="space-y-2" noValidate>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          value={code}
          onChange={(event) => { setCode(event.target.value); setError(null); }}
          placeholder="CERT-…"
          aria-label="Mã chứng chỉ"
          aria-invalid={Boolean(error)}
          className="h-11 font-mono uppercase"
          autoCapitalize="characters"
          spellCheck={false}
        />
        <Button type="submit" size="lg" className="h-11 px-5">
          <SearchCheckIcon /> Xác minh
        </Button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </form>
  );
}
