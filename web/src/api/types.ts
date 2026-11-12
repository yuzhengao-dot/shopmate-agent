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

export type ToolPayload =
  | { type: 'products'; items: Product[] }
  | { type: 'orders'; items: Order[] }
  | { type: 'ticket'; item: AfterSaleTicket };

export interface ToolTrace {
  id: string;
  name: string;
  label: string;
  status: 'running' | 'done' | 'error';
  summary?: string;
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