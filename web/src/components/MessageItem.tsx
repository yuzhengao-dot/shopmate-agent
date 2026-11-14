import type { ChatMessage } from '@/api/types';
import { RichText } from './RichText';
import { ProductCard } from './ProductCard';
import { OrderCard, TicketCard, AfterSaleDraftCard } from './OrderCard';
import { KnowledgeCard } from './KnowledgeCard';

interface MessageItemProps {
  message: ChatMessage;
  streaming: boolean;
  onAsk: (message: string) => void;
}

export function MessageItem({ message, streaming, onAsk }: MessageItemProps) {
  if (message.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] rounded-lg rounded-br-sm bg-ink-100 px-4 py-2.5 text-[15px] leading-7 text-ink-800">
          {message.content}
        </div>
      </div>
    );
  }

  const cards = message.tools.filter((tool) => Boolean(tool.payload));

  return (
    <div className="flex gap-3">
      <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-ink-900 text-[11px] font-medium text-white">
        满
      </div>
      <div className="min-w-0 flex-1">
        {message.content ? <RichText text={message.content} /> : null}

        {!message.content && streaming ? (
          <div className="flex h-7 items-center gap-1">
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-ink-300 [animation-delay:-0.2s]" />
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-ink-300 [animation-delay:-0.1s]" />
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-ink-300" />
          </div>
        ) : null}

        {message.error ? (
          <div className="mt-2 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-xs leading-5 text-rose-700">
            {message.error}
          </div>
        ) : null}

        {cards.map((tool) => {
          const payload = tool.payload!;
          if (payload.type === 'products') {
            return (
              <div key={tool.id} className="mt-3 space-y-2">
                {payload.items.map((product) => (
                  <ProductCard key={`${tool.id}-${product.id}`} product={product} onAsk={onAsk} />
                ))}
              </div>
            );
          }
          if (payload.type === 'orders') {
            return (
              <div key={tool.id} className="mt-3 space-y-2">
                {payload.items.map((order) => (
                  <OrderCard key={`${tool.id}-${order.orderNo}`} order={order} onAsk={onAsk} />
                ))}
              </div>
            );
          }
          if (payload.type === 'knowledge') {
            return (
              <div key={tool.id} className="mt-3">
                <KnowledgeCard hits={payload.items} />
              </div>
            );
          }
          if (payload.type === 'afterSaleDraft') {
            return (
              <div key={tool.id} className="mt-3">
                <AfterSaleDraftCard draft={payload.item} />
              </div>
            );
          }
          return (
            <div key={tool.id} className="mt-3">
              <TicketCard ticket={payload.item} />
            </div>
          );
        })}
      </div>
    </div>
  );
}