import { GraduationCapIcon } from "lucide-react";
import Link from "next/link";

export function Brand({ href = "/" }: { href?: string }) {
  return (
    <Link href={href} className="flex items-center gap-2 font-semibold tracking-tight">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <GraduationCapIcon className="size-4.5" />
      </span>
      <span>
        HUNRE <span className="text-muted-foreground">Learning</span>
      </span>
    </Link>
  );
}
