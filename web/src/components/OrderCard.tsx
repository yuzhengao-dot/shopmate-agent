import { useState } from 'react';
import type { AfterSaleDraft, AfterSaleTicket, Order } from '@/api/types';
import { createAfterSale } from '@/api/client';
import { money, statusTone } from '@/lib/utils';

interface OrderCardProps {
  order: Order;
  onAsk?: (message: string) => void;
  onAfterSale?: (order: Order) => void;
}

export function OrderCard({ order, onAsk, onAfterSale }: OrderCardProps) {
  const latest = order.tracking[order.tracking.length - 1];
  return (
    <div className="rounded-lg border border-ink-200 bg-white">
      <div className="flex items-center justify-between border-b border-ink-100 px-3.5 py-2.5">
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs text-ink-600">{order.orderNo}</span>
          <span className={`rounded border px-1.5 py-0.5 text-[11px] ${statusTone(order.status)}`}>
            {order.statusText}
          </span>
        </div>
        <span className="text-xs text-ink-400">{order.createdAt}</span>
      </div>

      <div className="space-y-1.5 px-3.5 py-3">
        {order.items.map((item) => (
          <div key={`${order.orderNo}-${item.productName}`} className="flex items-center justify-between gap-3 text-sm">
            <span className="truncate text-ink-700">
              {item.productName}
              <span className="ml-1 text-xs text-ink-400">×{item.quantity}</span>
            </span>
            <span className="shrink-0 tabular-nums text-ink-600">{money(item.price)}</span>
          </div>
        ))}
      </div>

      {latest ? (
        <div className="border-t border-ink-100 px-3.5 py-2 text-xs text-ink-500">
          <span className="text-ink-400">{latest.time}</span>
          <span className="ml-2">{latest.text}</span>
        </div>
      ) : null}

      <div className="flex items-center justify-between gap-3 border-t border-ink-100 px-3.5 py-2.5">
        <div className="text-xs text-ink-500">
          合计 <span className="ml-1 text-sm font-semibold text-ink-900">{money(order.totalAmount)}</span>
        </div>
        <div className="flex items-center gap-2">
          {onAsk ? (
            <button
              type="button"
              onClick={() => onAsk(`帮我查一下订单 ${order.orderNo} 的物流`)}
              className="rounded border border-ink-200 px-2 py-1 text-xs text-ink-600 transition-colors hover:border-accent-200 hover:text-accent-600"
            >
              查物流
            </button>
          ) : null}
          {onAfterSale ? (
            <button
              type="button"
              onClick={() => onAfterSale(order)}
              className="rounded border border-ink-200 px-2 py-1 text-xs text-ink-600 transition-colors hover:border-rose-200 hover:text-rose-600"
            >
              申请售后
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function TicketCard({ ticket }: { ticket: AfterSaleTicket }) {
  return (
    <div className="rounded-lg border border-emerald-200 bg-emerald-50/60 p-3.5">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-emerald-800">售后工单已创建</span>
        <span className="rounded border border-emerald-200 bg-white px-1.5 py-0.5 text-[11px] text-emerald-700">
          {ticket.status}
        </span>
      </div>
      <div className="mt-2 space-y-1 text-xs text-emerald-900/80">
        <p>
          工单号 <span className="font-mono">{ticket.ticketNo}</span>
        </p>
        <p>
          订单 <span className="font-mono">{ticket.orderNo}</span> · {ticket.type}
        </p>
        <p>原因：{ticket.reason}</p>
      </div>
    </div>
  );
}

/** 售后草稿确认卡：只有用户点击「确认提交」才会真正建单 */
export function AfterSaleDraftCard({ draft }: { draft: AfterSaleDraft }) {
  const [state, setState] = useState<'idle' | 'submitting' | 'done' | 'cancelled'>('idle');
  const [ticket, setTicket] = useState<AfterSaleTicket | null>(null);
  const [error, setError] = useState('');

  if (state === 'done' && ticket) {
    return <TicketCard ticket={ticket} />;
  }

  const confirm = async () => {
    setState('submitting');
    setError('');
    try {
      const created = await createAfterSale(draft.orderNo, draft.type, draft.reason);
      setTicket(created);
      setState('done');
    } catch (e) {
      setError((e as Error).message);
      setState('idle');
    }
  };

  return (
    <div className="rounded-lg border border-amber-200 bg-amber-50/60 p-3.5">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-amber-900">售后申请待确认</span>
        <span className="rounded border border-amber-200 bg-white px-1.5 py-0.5 text-[11px] text-amber-700">
          未提交
        </span>
      </div>
      <div className="mt-2 space-y-1 text-xs text-amber-900/80">
        <p>
          订单 <span className="font-mono">{draft.orderNo}</span> · {draft.statusText}
        </p>
        <p>
          {draft.productName ? `${draft.productName} · ` : ''}
          {draft.type} · {money(draft.totalAmount)}
        </p>
        <p>原因：{draft.reason}</p>
      </div>

      {state === 'cancelled' ? (
        <p className="mt-2.5 text-xs text-ink-500">已取消，未创建任何工单。</p>
      ) : (
        <div className="mt-3 flex items-center gap-2">
          <button
            type="button"
            onClick={confirm}
            disabled={state === 'submitting'}
            className="rounded bg-ink-900 px-2.5 py-1 text-xs text-white transition-opacity hover:opacity-85 disabled:opacity-50"
          >
            {state === 'submitting' ? '提交中…' : '确认提交'}
          </button>
          <button
            type="button"
            onClick={() => setState('cancelled')}
            disabled={state === 'submitting'}
            className="rounded border border-ink-200 bg-white px-2.5 py-1 text-xs text-ink-600 transition-colors hover:text-ink-900 disabled:opacity-50"
          >
            取消
          </button>
          <span className="text-[11px] text-ink-400">确认后才会真正建单</span>
        </div>
      )}

      {error ? <p className="mt-2 text-xs text-rose-600">{error}</p> : null}
    </div>
  );
}