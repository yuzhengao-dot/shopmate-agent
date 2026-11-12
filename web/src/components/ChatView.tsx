import { useEffect, useRef } from 'react';
import type { ChatMessage, Meta } from '@/api/types';
import { MessageItem } from './MessageItem';
import { Composer } from './Composer';

interface ChatViewProps {
  messages: ChatMessage[];
  streaming: boolean;
  meta: Meta | null;
  draft: string;
  suggestions: string[];
  onDraftChange: (value: string) => void;
  onSend: (text: string) => void;
  onStop: () => void;
}

export function ChatView({
  messages,
  streaming,
  meta,
  draft,
  suggestions,
  onDraftChange,
  onSend,
  onStop,
}: ChatViewProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: streaming ? 'auto' : 'smooth', block: 'end' });
  }, [messages, streaming]);

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="min-h-0 flex-1 overflow-y-auto px-4 py-6 md:px-8">
        <div className="mx-auto flex max-w-3xl flex-col gap-6">
          {messages.length === 0 ? (
            <div className="pt-6">
              <p className="font-mono text-[11px] uppercase tracking-widest text-ink-400">ShopMate Agent</p>
              <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink-900">今天想买点什么？</h1>
              <p className="mt-2 max-w-xl text-sm leading-6 text-ink-500">
                我是导购助手小满。可以按预算挑商品、查订单物流、申请退货换货。当前为
                {meta?.mode === 'llm' ? `在线模型（${meta.model}）` : '演示模式（未配置大模型密钥，规则引擎驱动）'}。
              </p>
              <div className="mt-6 grid gap-2 sm:grid-cols-2">
                {suggestions.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => onSend(item)}
                    className="rounded-lg border border-ink-200 bg-white px-3.5 py-3 text-left text-[13px] leading-5 text-ink-600 transition-colors hover:border-ink-300 hover:text-ink-900"
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((message) => (
              <MessageItem
                key={message.id}
                message={message}
                streaming={streaming && message.id === messages[messages.length - 1].id}
                onAsk={(text) => onDraftChange(text)}
              />
            ))
          )}
          <div ref={bottomRef} />
        </div>
      </div>

      <Composer value={draft} onChange={onDraftChange} onSend={onSend} onStop={onStop} streaming={streaming} />
    </div>
  );
}