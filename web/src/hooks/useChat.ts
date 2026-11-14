import { useCallback, useEffect, useRef, useState } from 'react';
import { streamChat } from '@/api/client';
import type { ChatMessage, Session, ToolTrace } from '@/api/types';
import { loadSessions, saveSessions, uid } from '@/lib/utils';

interface ChatState {
  sessions: Session[];
  activeId: string;
}

function blankSession(): Session {
  return { id: uid(), title: '新的对话', messages: [], updatedAt: Date.now() };
}

function upsertTool(tools: ToolTrace[], raw: Record<string, any>): ToolTrace[] {
  const id = String(raw.id ?? `tool-${tools.length}`);
  const index = tools.findIndex((item) => item.id === id);
  const existing = index >= 0 ? tools[index] : undefined;
  const next: ToolTrace = {
    id,
    name: String(raw.name ?? existing?.name ?? 'tool'),
    label: String(raw.label ?? existing?.label ?? raw.name ?? '工具调用'),
    status: (raw.status as ToolTrace['status']) ?? 'running',
    summary: raw.summary ? String(raw.summary) : existing?.summary,
    durationMs: typeof raw.durationMs === 'number' ? raw.durationMs : existing?.durationMs,
    payload: (raw.payload as ToolTrace['payload']) ?? existing?.payload,
  };
  if (index < 0) return [...tools, next];
  const copy = [...tools];
  copy[index] = next;
  return copy;
}

export function useChat() {
  const [state, setState] = useState<ChatState>(() => {
    const stored = loadSessions();
    const list = stored.length ? stored : [blankSession()];
    return { sessions: list, activeId: list[0].id };
  });
  const [streaming, setStreaming] = useState(false);

  const activeRef = useRef(state.activeId);
  activeRef.current = state.activeId;
  const streamingRef = useRef(false);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    saveSessions(state.sessions);
  }, [state.sessions]);

  const patchAssistant = useCallback((sessionId: string, messageId: string, fn: (msg: ChatMessage) => ChatMessage) => {
    setState((prev) => ({
      ...prev,
      sessions: prev.sessions.map((session) =>
        session.id !== sessionId
          ? session
          : {
              ...session,
              updatedAt: Date.now(),
              messages: session.messages.map((msg) => (msg.id === messageId ? fn(msg) : msg)),
            },
      ),
    }));
  }, []);

  const send = useCallback(
    async (raw: string) => {
      const text = raw.trim();
      if (!text || streamingRef.current) return;
      const sessionId = activeRef.current;
      const userMsg: ChatMessage = {
        id: `u-${Date.now()}`,
        role: 'user',
        content: text,
        tools: [],
        createdAt: Date.now(),
      };
      const assistantMsg: ChatMessage = {
        id: `a-${Date.now()}`,
        role: 'assistant',
        content: '',
        tools: [],
        createdAt: Date.now(),
      };

      setState((prev) => ({
        ...prev,
        sessions: prev.sessions.map((session) =>
          session.id !== sessionId
            ? session
            : {
                ...session,
                title: session.messages.length === 0 ? text.slice(0, 18) : session.title,
                updatedAt: Date.now(),
                messages: [...session.messages, userMsg, assistantMsg],
              },
        ),
      }));

      streamingRef.current = true;
      setStreaming(true);
      const controller = new AbortController();
      abortRef.current = controller;

      const patch = (fn: (msg: ChatMessage) => ChatMessage) => patchAssistant(sessionId, assistantMsg.id, fn);

      try {
        await streamChat(
          sessionId,
          text,
          (evt) => {
            if (evt.event === 'token' && evt.json?.text) {
              const delta = String(evt.json.text);
              patch((msg) => ({ ...msg, content: msg.content + delta }));
            } else if (evt.event === 'tool' && evt.json) {
              const rawTool = evt.json;
              patch((msg) => ({ ...msg, tools: upsertTool(msg.tools, rawTool) }));
            } else if (evt.event === 'error' && evt.json) {
              patch((msg) => ({ ...msg, error: String(evt.json?.message ?? '对话失败') }));
            }
          },
          controller.signal,
        );
      } catch (error) {
        const err = error as Error;
        if (err.name !== 'AbortError') {
          patch((msg) => ({ ...msg, error: err.message }));
        }
      } finally {
        streamingRef.current = false;
        setStreaming(false);
        abortRef.current = null;
      }
    },
    [patchAssistant],
  );

  const stop = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    streamingRef.current = false;
    setStreaming(false);
  }, []);

  const newSession = useCallback(() => {
    const session = blankSession();
    setState((prev) => ({ sessions: [session, ...prev.sessions], activeId: session.id }));
  }, []);

  const selectSession = useCallback((id: string) => {
    setState((prev) => ({ ...prev, activeId: id }));
  }, []);

  const removeSession = useCallback((id: string) => {
    setState((prev) => {
      const rest = prev.sessions.filter((item) => item.id !== id);
      const list = rest.length ? rest : [blankSession()];
      const activeId = prev.activeId === id ? list[0].id : prev.activeId;
      return { sessions: list, activeId };
    });
  }, []);

  const activeSession = state.sessions.find((item) => item.id === state.activeId) ?? state.sessions[0];

  return {
    sessions: state.sessions,
    activeId: state.activeId,
    messages: activeSession?.messages ?? [],
    title: activeSession?.title ?? '',
    streaming,
    send,
    stop,
    newSession,
    selectSession,
    removeSession,
  };
}