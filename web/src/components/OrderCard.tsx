import type { AfterSaleTicket, Order } from '@/api/types';
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