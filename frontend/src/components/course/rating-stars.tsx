import { StarIcon } from "lucide-react";

/** Tô theo điểm thực tế: 4,5 là bốn sao đầy và một nửa sao. */
export function RatingStars({ rating }: { rating: number }) {
  const value = Math.min(5, Math.max(0, rating));
  return (
    <span className="inline-flex gap-1" role="img" aria-label={`${value} trên 5 sao`}>
      {[0, 1, 2, 3, 4].map((index) => (
        <span key={index} className="relative block size-5" aria-hidden>
          <StarIcon className="size-5 opacity-30" />
          <span
            className="absolute inset-y-0 left-0 overflow-hidden"
            style={{
              width: `${Math.min(1, Math.max(0, value - index)) * 100}%`,
            }}
          >
            <StarIcon className="size-5 fill-current" />
          </span>
        </span>
      ))}
    </span>
  );
}
