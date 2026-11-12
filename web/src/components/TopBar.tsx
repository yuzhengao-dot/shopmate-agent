import type { Meta } from '@/api/types';

export type Tab = 'chat' | 'catalog' | 'orders';

const TABS: { key: Tab; label: string }[] = [
  { key: 'chat', label: '对话' },
  { key: 'catalog', label: '商品库' },
  { key: 'orders', label: '我的订单' },
];

interface TopBarProps {
  tab: Tab;
  meta: Meta | null;
  panelOpen: boolean;
  onTab: (tab: Tab) => void;
  onTogglePanel: () => void;
  onToggleSidebar: () => void;
}

export function TopBar({ tab, meta, panelOpen, onTab, onTogglePanel, onToggleSidebar }: TopBarProps) {
  return (
    <header className="flex h-14 shrink-0 items-center gap-3 border-b border-ink-200 bg-white px-4 md:px-6">
      <button
        type="button"
        aria-label="打开会话列表"
        onClick={onToggleSidebar}
        className="rounded-md p-1.5 text-ink-500 hover:bg-ink-50 md:hidden"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        </svg>
      </button>

      <nav className="flex items-center gap-1">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            onClick={() => onTab(item.key)}
            className={`rounded-md px-2.5 py-1.5 text-[13px] transition-colors ${
              tab === item.key ? 'bg-ink-100 font-medium text-ink-900' : 'text-ink-500 hover:text-ink-800'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <div className="ml-auto flex items-center gap-2">
        <span
          className={`hidden items-center gap-1.5 rounded border px-2 py-1 text-[11px] sm:flex ${
            meta?.mode === 'llm'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-amber-200 bg-amber-50 text-amber-700'
          }`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${meta?.mode === 'llm' ? 'bg-emerald-500' : 'bg-amber-500'}`} />
          {meta ? (meta.mode === 'llm' ? `LLM · ${meta.model}` : '演示模式') : '连接中'}
        </span>
        <button
          type="button"
          onClick={onTogglePanel}
          className={`rounded-md border px-2.5 py-1.5 text-[12px] transition-colors ${
            panelOpen ? 'border-ink-300 text-ink-700' : 'border-ink-200 text-ink-400 hover:text-ink-700'
          }`}
        >
          工具轨迹
        </button>
      </div>
    </header>
  );
}