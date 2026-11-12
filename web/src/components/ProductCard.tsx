import type { Product } from '@/api/types';
import { money } from '@/lib/utils';

export function ProductCard({ product, onAsk }: { product: Product; onAsk?: (message: string) => void }) {
  return (
    <div className="rounded-lg border border-ink-200 bg-white p-3.5 transition-colors hover:border-ink-300">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-ink-100 text-sm font-medium text-ink-500">
          {product.brand.slice(0, 1)}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <p className="truncate text-sm font-medium text-ink-800">{product.name}</p>
            <span className="shrink-0 text-xs text-ink-400">★ {Number(product.rating).toFixed(1)}</span>
          </div>
          <div className="mt-1.5 flex flex-wrap gap-1.5">
            {product.tags.slice(0, 3).map((tag) => (
              <span key={tag} className="rounded border border-ink-200 px-1.5 py-0.5 text-[11px] text-ink-500">
                {tag}
              </span>
            ))}
          </div>
          <p className="mt-2 line-clamp-2 text-xs leading-5 text-ink-500">{product.summary}</p>
          <div className="mt-2.5 flex items-end justify-between gap-2">
            <div className="flex items-baseline gap-2">
              <span className="text-base font-semibold text-ink-900">{money(product.price)}</span>
              {product.originalPrice ? (
                <span className="text-xs text-ink-400 line-through">{money(product.originalPrice)}</span>
              ) : null}
            </div>
            <div className="flex items-center gap-2">
              <span className="text-[11px] text-ink-400">库存 {product.stock}</span>
              {onAsk ? (
                <button
                  type="button"
                  onClick={() => onAsk(`介绍一下「${product.name}」，适合什么人？`)}
                  className="rounded border border-ink-200 px-2 py-1 text-xs text-ink-600 transition-colors hover:border-accent-200 hover:text-accent-600"
                >
                  问小满
                </button>
              ) : null}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}