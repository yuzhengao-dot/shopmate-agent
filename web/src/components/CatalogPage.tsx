import { useEffect, useMemo, useState } from 'react';
import { getProducts } from '@/api/client';
import type { Product } from '@/api/types';
import { ProductCard } from '@/components/ProductCard';

export function CatalogPage({ onAsk }: { onAsk: (message: string) => void }) {
  const [products, setProducts] = useState<Product[]>([]);
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('全部');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getProducts()
      .then((list) => setProducts(list))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(() => ['全部', ...Array.from(new Set(products.map((item) => item.category)))], [products]);

  const filtered = useMemo(() => {
    const key = keyword.trim().toLowerCase();
    return products.filter((item) => {
      const inCategory = category === '全部' || item.category === category;
      const text = `${item.name}${item.brand}${item.tags.join('')}${item.summary}`.toLowerCase();
      return inCategory && (!key || text.includes(key));
    });
  }, [products, keyword, category]);

  return (
    <div className="min-h-0 flex-1 overflow-y-auto px-4 py-6 md:px-8">
      <div className="mx-auto max-w-5xl">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold tracking-tight text-ink-900">商品库</h2>
            <p className="mt-1 text-xs text-ink-400">
              共 {products.length} 件在售商品，Agent 的 searchProducts 工具查询的就是这份数据
            </p>
          </div>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索商品名、品牌、标签"
            className="w-full max-w-64 rounded-md border border-ink-200 px-3 py-2 text-sm text-ink-700 placeholder:text-ink-300 focus:border-ink-400 focus:outline-none sm:w-64"
          />
        </div>

        <div className="mt-4 flex flex-wrap gap-1.5">
          {categories.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setCategory(item)}
              className={`rounded border px-2.5 py-1 text-xs transition-colors ${
                category === item
                  ? 'border-ink-300 bg-ink-100 text-ink-800'
                  : 'border-ink-200 text-ink-500 hover:text-ink-800'
              }`}
            >
              {item}
            </button>
          ))}
        </div>

        {error ? (
          <p className="mt-8 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>
        ) : null}

        {loading ? (
          <p className="mt-8 text-sm text-ink-400">加载中…</p>
        ) : (
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            {filtered.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onAsk={(text) => {
                  onAsk(text);
                }}
              />
            ))}
          </div>
        )}

        {!loading && !error && filtered.length === 0 ? (
          <p className="mt-10 text-center text-sm text-ink-400">没有匹配的商品，换个关键词试试。</p>
        ) : null}
      </div>
    </div>
  );
}