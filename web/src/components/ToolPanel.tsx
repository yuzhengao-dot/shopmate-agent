import type { Meta, ToolTrace } from '@/api/types';

const STATUS_DOT: Record<ToolTrace['status'], string> = {
  running: 'bg-amber-500 animate-pulse',
  done: 'bg-emerald-500',
  error: 'bg-rose-500',
};

export function ToolPanel({ tools, meta, messageCount }: { tools: ToolTrace[]; meta: Meta | null; messageCount: number }) {
  const durations = tools
    .map((tool) => tool.durationMs)
    .filter((value): value is number => typeof value === 'number');
  const avgDuration = durations.length
    ? Math.round(durations.reduce((sum, value) => sum + value, 0) / durations.length)
    : null;

  return (
    <aside className="flex h-full w-full flex-col border-l border-ink-200 bg-ink-50/60">
      <div className="flex items-center justify-between border-b border-ink-200 px-4 py-3">
        <span className="text-xs font-medium tracking-wide text-ink-500">工具调用轨迹</span>
        <span className="font-mono text-[11px] text-ink-400">{tools.length}</span>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-3">
        {tools.length === 0 ? (
          <p className="rounded-md border border-dashed border-ink-200 px-3 py-6 text-center text-xs leading-6 text-ink-400">
            本轮对话尚未调用工具。
            <br />
            试试「预算 3000 元以内推荐手机」或「我的订单到哪了」。
          </p>
        ) : (
          <ol className="relative space-y-3 border-l border-ink-200 pl-4">
            {tools.map((tool) => (
              <li key={tool.id} className="relative">
                <span className={`absolute -left-[21px] top-1.5 h-2 w-2 rounded-full ${STATUS_DOT[tool.status]}`} />
                <p className="text-xs font-medium text-ink-700">{tool.label}</p>
                <p className="font-mono text-[11px] text-ink-400">
                  {tool.name}
                  {typeof tool.durationMs === 'number' ? (
                    <span className="ml-1 text-ink-300">· {tool.durationMs}ms</span>
                  ) : null}
                </p>
                {tool.summary ? <p className="mt-1 text-[11px] leading-5 text-ink-500">{tool.summary}</p> : null}
              </li>
            ))}
          </ol>
        )}
      </div>

      <div className="border-t border-ink-200 px-4 py-3 text-[11px] leading-5 text-ink-400">
        <div className="flex items-center justify-between">
          <span>模式</span>
          <span className={meta?.mode === 'llm' ? 'text-emerald-600' : 'text-amber-600'}>
            {meta?.mode === 'llm' ? 'LLM 在线' : '演示模式'}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span>模型</span>
          <span className="font-mono">{meta?.model ?? '—'}</span>
        </div>
        <div className="flex items-center justify-between">
          <span>工具调用</span>
          <span className="font-mono">
            {tools.length} 次{avgDuration !== null ? ` · 均 ${avgDuration}ms` : ''}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span>会话消息</span>
          <span className="font-mono">{messageCount}</span>
        </div>
      </div>
    </aside>
  );
}