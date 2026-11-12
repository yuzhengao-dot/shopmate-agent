import type { Session } from '@/api/types';

const KEY = 'shopmate.sessions.v1';

export function loadSessions(): Session[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((item) => item && typeof item.id === 'string' && Array.isArray(item.messages));
  } catch {
    return [];
  }
}

export function saveSessions(sessions: Session[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(sessions));
  } catch {
    // 本地存储不可用时忽略
  }
}

export function uid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `s-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

export function money(value: number | null | undefined): string {
  const num = Number(value ?? 0);
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function shortTime(ts: number): string {
  const date = new Date(ts);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  return sameDay
    ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

export function statusTone(status: string): string {
  switch (status) {
    case 'PAID':
      return 'bg-amber-50 text-amber-700 border-amber-200';
    case 'SHIPPED':
      return 'bg-accent-50 text-accent-600 border-accent-200';
    case 'DELIVERED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case 'COMPLETED':
      return 'bg-ink-100 text-ink-600 border-ink-200';
    default:
      return 'bg-ink-100 text-ink-500 border-ink-200';
  }
}