import { useCallback, useEffect, useState } from 'react';
import { createAfterSale, getOrders } from '@/api/client';
import type { Order } from '@/api/types';
import { OrderCard } from '@/components/OrderCard';

const FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'PAID', label: '待发货' },
  { key: 'SHIPPED', label: '已发货' },
  { key: 'DELIVERED', label: '已签收' },
  { key: 'COMPLETED', label: '已完成' },
];

export function OrdersPage({ onAsk }: { onAsk: (message: string) => void }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [filter, setFilter] = useState('ALL');
  const [notice, setNotice] = useState<{ type: 'ok' | 'error'; text: string } | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async (status: string) => {
    try {
      setError('');
      setOrders(await getOrders(status));
    } catch (e) {
      setError((e as Error).message);
    }
  }, []);

  useEffect(() => {
    void load(filter);
  }, [filter, load]);

  const afterSale = async (order: Order) => {
    try {
      const ticket = await createAfterSale(order.orderNo, '退货退款', '页面快捷申请：商品与描述不符');
      setNotice({ type: 'ok', text: `订单 ${ticket.orderNo} 的售后工单已创建：${ticket.ticketNo}（${ticket.status}）` });
      await load(filter);
    } catch (e) {
      setNotice({ type: 'error', text: (e as Error).message });
    }
  };

  return (
    <div className="min-h-0 flex-1 overflow-y-auto px-4 py-6 md:px-8">
      <div className="mx-auto max-w-3xl">
        <h2 className="text-lg font-semibold tracking-tight text-ink-900">我的订单</h2>
        <p className="mt-1 text-xs text-ink-400">数据来自 H2 示例库，Agent 的订单/物流/售后工具与页面共用同一份数据</p>

        <div className="mt-4 flex flex-wrap gap-1.5">
          {FILTERS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setFilter(item.key)}
              className={`rounded border px-2.5 py-1 text-xs transition-colors ${
                filter === item.key
                  ? 'border-ink-300 bg-ink-100 text-ink-800'
                  : 'border-ink-200 text-ink-500 hover:text-ink-800'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>

        {notice ? (
          <div
            className={`mt-4 rounded-md border px-3 py-2 text-xs leading-5 ${
              notice.type === 'ok'
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                : 'border-rose-200 bg-rose-50 text-rose-700'
            }`}
          >
            {notice.text}
          </div>
        ) : null}

        {error ? (
          <p className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>
        ) : null}

        <div className="mt-4 space-y-3 pb-6">
          {orders.map((order) => (
            <OrderCard
              key={order.orderNo}
              order={order}
              onAsk={(text) => onAsk(text)}
              onAfterSale={afterSale}
            />
          ))}
          {!error && orders.length === 0 ? (
            <p className="py-10 text-center text-sm text-ink-400">该状态下暂无订单。</p>
          ) : null}
        </div>
      </div>
    </div>
  );
}