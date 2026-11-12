import type { AfterSaleTicket, ChatEvent, Meta, Order, Product } from './types';

const BASE = (import.meta.env.VITE_API_URL as string) || '';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init.headers ?? {}) },
    });
  } catch {
    throw new Error(`无法连接后端（${BASE || '代理 /api'}），请确认 Java 服务已在 8080 端口启动`);
  }

  const text = await res.text();
  let body: any = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    throw new Error(`接口返回非 JSON 内容（HTTP ${res.status}）`);
  }
  if (!res.ok) {
    throw new Error(body?.message || `请求失败（HTTP ${res.status}）`);
  }
  return body as T;
}

export function getMeta(): Promise<Meta> {
  return request<Meta>('/api/meta');
}

export function getProducts(keyword?: string, category?: string): Promise<Product[]> {
  const params = new URLSearchParams();
  if (keyword?.trim()) params.set('keyword', keyword.trim());
  if (category) params.set('category', category);
  const query = params.toString();
  return request<Product[]>(`/api/products${query ? `?${query}` : ''}`);
}

export function getOrders(status = 'ALL'): Promise<Order[]> {
  return request<Order[]>(`/api/orders?status=${encodeURIComponent(status)}`);
}

export function createAfterSale(orderNo: string, type: string, reason: string): Promise<AfterSaleTicket> {
  return request<AfterSaleTicket>(`/api/orders/${encodeURIComponent(orderNo)}/after-sale`, {
    method: 'POST',
    body: JSON.stringify({ type, reason }),
  });
}

function parseEvent(raw: string): ChatEvent | null {
  let event = 'message';
  const dataLines: string[] = [];
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
  }
  if (!dataLines.length) return null;
  const data = dataLines.join('\n');
  let json: Record<string, any> | undefined;
  try {
    json = JSON.parse(data);
  } catch {
    json = undefined;
  }
  return { event, data, json };
}

/** POST /api/chat 并逐事件回调：meta / token / tool / error / done */
export async function streamChat(
  sessionId: string,
  message: string,
  onEvent: (evt: ChatEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  let res: Response;
  try {
    res = await fetch(`${BASE}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify({ sessionId, message }),
      signal,
    });
  } catch (err) {
    if ((err as Error)?.name === 'AbortError') return;
    throw new Error(`无法连接后端（${BASE || '代理 /api'}），请确认 Java 服务已在 8080 端口启动`);
  }

  if (!res.ok || !res.body) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `对话请求失败（HTTP ${res.status}）`);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  const flush = (chunk: string) => {
    const evt = parseEvent(chunk);
    if (evt) onEvent(evt);
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let idx = buffer.indexOf('\n\n');
    while (idx >= 0) {
      flush(buffer.slice(0, idx));
      buffer = buffer.slice(idx + 2);
      idx = buffer.indexOf('\n\n');
    }
  }
  if (buffer.trim()) flush(buffer);
}