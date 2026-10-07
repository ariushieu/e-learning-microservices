import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="flex min-h-svh flex-col">
      <div className="h-16 border-b bg-card" />
      <div className="mx-auto w-full max-w-5xl space-y-6 px-4 py-8 sm:px-6 lg:px-10">
        <Skeleton className="h-4 w-48" />
        <Skeleton className="h-8 w-80 max-w-full" />
        <Skeleton className="aspect-video w-full rounded-xl" />
      </div>
    </div>
  );
}
