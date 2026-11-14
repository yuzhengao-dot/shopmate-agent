export interface Product {
  id: number;
  name: string;
  brand: string;
  category: string;
  price: number;
  originalPrice: number | null;
  rating: number;
  stock: number;
  tags: string[];
  summary: string;
}

export interface OrderItem {
  productName: string;
  quantity: number;
  price: number;
}

export interface TrackingNode {
  time: string;
  text: string;
}

export interface Order {
  orderNo: string;
  status: string;
  statusText: string;
  createdAt: string;
  totalAmount: number;
  receiver: string;
  address: string;
  carrier: string;
  logisticsNo: string;
  items: OrderItem[];
  tracking: TrackingNode[];
}

export interface AfterSaleTicket {
  ticketNo: string;
  orderNo: string;
  type: string;
  reason: string;
  status: string;
  createdAt: string;
}

/** 售后草稿：仅预检，未写入数据库，需要用户在前端确认 */
export interface AfterSaleDraft {
  orderNo: string;
  statusText: string;
  type: string;
  reason: string;
  totalAmount: number;
  productName: string;
}

/** 知识库命中：内容来自平台规则库，由 BM25 检索返回 */
export interface KnowledgeHit {
  id: number;
  title: string;
  category: string;
  content: string;
  snippet: string;
  /** 相对相关度，0~1 */
  match: number;
}

export type ToolPayload =
  | { type: 'products'; items: Product[] }
  | { type: 'orders'; items: Order[] }
  | { type: 'knowledge'; items: KnowledgeHit[] }
  | { type: 'ticket'; item: AfterSaleTicket }
  | { type: 'afterSaleDraft'; item: AfterSaleDraft };

export interface ToolTrace {
  id: string;
  name: string;
  label: string;
  status: 'running' | 'done' | 'error';
  summary?: string;
  /** 服务端记录的本次执行耗时（毫秒），未结束时没有该字段 */
  durationMs?: number;
  payload?: ToolPayload;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  tools: ToolTrace[];
  error?: string;
  createdAt: number;
}

export interface Session {
  id: string;
  title: string;
  messages: ChatMessage[];
  updatedAt: number;
}

export interface Meta {
  mode: 'llm' | 'demo';
  model: string;
  user: { id: string; name: string };
  suggestions: string[];
  categories: string[];
  productCount: number;
  orderCount: number;
}

export interface ChatEvent {
  event: string;
  data: string;
  json?: Record<string, any>;
}