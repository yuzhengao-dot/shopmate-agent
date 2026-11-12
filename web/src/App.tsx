import { useEffect, useMemo, useState } from 'react';
import { getMeta } from '@/api/client';
import type { Meta } from '@/api/types';
import { useChat } from '@/hooks/useChat';
import { Sidebar } from '@/components/Sidebar';
import { TopBar, type Tab } from '@/components/TopBar';
import { ChatView } from '@/components/ChatView';
import { ToolPanel } from '@/components/ToolPanel';
import { CatalogPage } from '@/components/CatalogPage';
import { OrdersPage } from '@/components/OrdersPage';

const FALLBACK_SUGGESTIONS = [
  '预算 3000 元以内，推荐一款拍照好的手机',
  '我的订单到哪了',
  '降噪耳机有什么推荐',
  '我想退掉 SM20261028002 这单',
];

export default function App() {
  const [meta, setMeta] = useState<Meta | null>(null);
  const [tab, setTab] = useState<Tab>('chat');
  const [draft, setDraft] = useState('');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [panelOpen, setPanelOpen] = useState(true);
  const chat = useChat();

  useEffect(() => {
    getMeta()
      .then((data) => setMeta(data))
      .catch(() => setMeta(null));
  }, []);

  const lastAssistant = useMemo(
    () => [...chat.messages].reverse().find((message) => message.role === 'assistant'),
    [chat.messages],
  );

  const send = (text: string) => {
    setTab('chat');
    setDraft('');
    void chat.send(text);
  };

  const ask = (text: string) => {
    setDraft(text);
    setTab('chat');
  };

  return (
    <div className="relative flex h-screen overflow-hidden bg-white text-ink-800">
      <Sidebar
        sessions={chat.sessions}
        activeId={chat.activeId}
        meta={meta}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        onNew={() => {
          chat.newSession();
          setTab('chat');
          setSidebarOpen(false);
        }}
        onSelect={(id) => {
          chat.selectSession(id);
          setTab('chat');
        }}
        onDelete={chat.removeSession}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar
          tab={tab}
          meta={meta}
          panelOpen={panelOpen}
          onTab={setTab}
          onTogglePanel={() => setPanelOpen((value) => !value)}
          onToggleSidebar={() => setSidebarOpen(true)}
        />

        <div className="flex min-h-0 flex-1">
          <main className="flex min-w-0 flex-1 flex-col">
            {tab === 'chat' ? (
              <ChatView
                messages={chat.messages}
                streaming={chat.streaming}
                meta={meta}
                draft={draft}
                suggestions={meta?.suggestions ?? FALLBACK_SUGGESTIONS}
                onDraftChange={setDraft}
                onSend={send}
                onStop={chat.stop}
              />
            ) : tab === 'catalog' ? (
              <CatalogPage onAsk={ask} />
            ) : (
              <OrdersPage onAsk={ask} />
            )}
          </main>

          {panelOpen ? (
            <div className="hidden w-72 shrink-0 lg:block">
              <ToolPanel tools={lastAssistant?.tools ?? []} meta={meta} messageCount={chat.messages.length} />
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}