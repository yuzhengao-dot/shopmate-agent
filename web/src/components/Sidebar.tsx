import type { Meta, Session } from '@/api/types';
import { shortTime } from '@/lib/utils';

interface SidebarProps {
  sessions: Session[];
  activeId: string;
  meta: Meta | null;
  open: boolean;
  onClose: () => void;
  onNew: () => void;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
}

export function Sidebar({ sessions, activeId, meta, open, onClose, onNew, onSelect, onDelete }: SidebarProps) {
  return (
    <>
      {open ? (
        <button
          type="button"
          aria-label="关闭会话列表"
          className="fixed inset-0 z-10 bg-ink-900/20 md:hidden"
          onClick={onClose}
        />
      ) : null}

      <aside
        className={`${
          open ? 'flex' : 'hidden'
        } absolute inset-y-0 left-0 z-20 w-64 shrink-0 flex-col border-r border-ink-200 bg-ink-50/80 md:relative md:flex`}
      >
        <div className="flex items-center gap-2.5 px-4 py-4">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-ink-900 text-[13px] font-semibold text-white">
            S
          </div>
          <div className="min-w-0">
            <p className="text-sm font-semibold tracking-tight text-ink-900">ShopMate</p>
            <p className="text-[11px] text-ink-400">电商导购 Agent</p>
          </div>
        </div>

        <div className="px-3 pb-3">
          <button
            type="button"
            onClick={onNew}
            className="w-full rounded-md border border-ink-200 bg-white px-3 py-2 text-left text-sm text-ink-700 transition-colors hover:border-ink-300 hover:bg-white"
          >
            + 新建对话
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-2 pb-3">
          <p className="px-2 pb-1.5 text-[11px] font-medium tracking-wide text-ink-400">最近对话</p>
          <ul className="space-y-0.5">
            {sessions.map((session) => {
              const active = session.id === activeId;
              const last = session.messages[session.messages.length - 1];
              return (
                <li key={session.id} className="group relative">
                  <button
                    type="button"
                    onClick={() => {
                      onSelect(session.id);
                      onClose();
                    }}
                    className={`w-full rounded-md px-2 py-2 text-left transition-colors ${
                      active ? 'bg-white shadow-sm ring-1 ring-ink-200' : 'hover:bg-white/70'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className={`truncate text-[13px] ${active ? 'text-ink-900' : 'text-ink-600'}`}>
                        {session.title || '新的对话'}
                      </span>
                      <span className="shrink-0 text-[10px] text-ink-300 group-hover:opacity-0">
                        {shortTime(session.updatedAt)}
                      </span>
                    </div>
                    {last ? (
                      <p className="mt-0.5 truncate text-[11px] text-ink-400">
                        {last.role === 'user' ? '你：' : '小满：'}
                        {last.content.slice(0, 22) || '…'}
                      </p>
                    ) : (
                      <p className="mt-0.5 text-[11px] text-ink-300">还没有消息</p>
                    )}
                  </button>
                  <button
                    type="button"
                    aria-label="删除对话"
                    onClick={(event) => {
                      event.stopPropagation();
                      onDelete(session.id);
                    }}
                    className="absolute right-2 top-2 hidden h-4 w-4 items-center justify-center rounded text-ink-400 hover:bg-ink-100 hover:text-rose-500 group-hover:flex"
                  >
                    ×
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        <div className="border-t border-ink-200 px-4 py-3 text-[11px] leading-5 text-ink-400">
          <div className="flex items-center justify-between">
            <span>当前用户</span>
            <span className="text-ink-600">
              {meta?.user.name ?? '林晓'} · {meta?.user.id ?? 'U1001'}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span>在售商品</span>
            <span className="font-mono text-ink-600">{meta?.productCount ?? '—'}</span>
          </div>
          <div className="flex items-center justify-between">
            <span>我的订单</span>
            <span className="font-mono text-ink-600">{meta?.orderCount ?? '—'}</span>
          </div>
        </div>
      </aside>
    </>
  );
}