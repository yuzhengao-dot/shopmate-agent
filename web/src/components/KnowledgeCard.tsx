import type { KnowledgeHit } from '@/api/types';

interface KnowledgeCardProps {
  hits: KnowledgeHit[];
}

/** 知识库命中卡片：把检索到的规则和出处摊开给用户看，回答不是凭空来的 */
export function KnowledgeCard({ hits }: KnowledgeCardProps) {
  return (
    <div className="rounded-lg border border-ink-200 bg-white">
      <div className="flex items-center justify-between border-b border-ink-100 px-3.5 py-2">
        <span className="text-xs font-medium text-ink-600">知识库检索</span>
        <span className="text-[11px] text-ink-400">命中 {hits.length} 条</span>
      </div>
      <div className="divide-y divide-ink-100">
        {hits.map((hit) => (
          <div key={hit.id} className="px-3.5 py-2.5">
            <div className="flex items-center gap-2">
              <span className="shrink-0 rounded bg-ink-100 px-1.5 py-0.5 text-[11px] text-ink-500">
                {hit.category}
              </span>
              <span className="truncate text-sm font-medium text-ink-800">{hit.title}</span>
              <span className="ml-auto shrink-0 text-[11px] tabular-nums text-ink-400">
                相关度 {Math.round(hit.match * 100)}%
              </span>
            </div>
            <p className="mt-1 text-xs leading-5 text-ink-500">{hit.snippet}</p>
          </div>
        ))}
      </div>
    </div>
  );
}