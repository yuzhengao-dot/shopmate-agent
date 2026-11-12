# ShopMate · 电商导购 Agent（Java + LangChain4j）

一个用 **Java 17 + Spring Boot 3 + LangChain4j** 实现的电商 AI 导购 / 客服 Agent 示例：
大模型通过 **工具调用（Function Calling）** 查询真实业务数据（商品库、订单、物流、售后工单），
并用 **SSE 流式**把「回答内容 + 工具调用轨迹 + 结构化卡片」实时推给前端。

> 这个仓库是照着 GitHub 上 `langchain4j-examples` 的 Agent 思路**逆向复刻并改造为电商场景**的完整项目，
> 不是脚手架：后端、数据、前端、流式协议都是可运行的。

---

## 功能

| 能力 | 说明 |
| --- | --- |
| 智能导购 | 按关键词 / 品类 / 价格区间检索在售商品，最多推荐 3 件，返回商品卡片 |
| 商品详情 | 点击卡片追问价格、评分、库存、卖点 |
| 订单查询 | 按状态查询当前用户订单，返回订单卡片（含明细） |
| 物流跟踪 | 按订单号或「最近一笔订单」查询承运商与最新轨迹 |
| 售后办理 | 校验订单状态后创建退换货 / 维修工单，返回工单卡片 |
| 流式对话 | SSE `meta / tool / token / error / done` 事件，逐字输出 + 工具轨迹实时上屏 |
| 双运行模式 | 配置了 LLM 密钥 → 真 Agent；没有密钥 → 内置规则引擎，同样跑完整工具链路 |

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、Spring JDBC、H2（内存库）、LangChain4j 1.23.0、`SseEmitter`
- 前端：React 18、TypeScript、Vite 5、Tailwind CSS 3（无额外 UI / 状态库，自写 Markdown 渲染）
- 模型接入：任何 **OpenAI 兼容** 网关（阿里云百炼 DashScope、DeepSeek、Ollama、vLLM…）

## 目录结构

```
shopmate-agent/
├─ server/                         Spring Boot 后端
│  └─ src/main/java/com/shopmate/agent/
│     ├─ agent/
│     │  ├─ ShopAssistant.java     AiServices 接口（@SystemMessage 人设 + 流式返回）
│     │  ├─ ShopTools.java         工具集（@Tool）：商品/订单/物流/售后
│     │  ├─ AgentFactory.java      按是否配置密钥装配 LLM Agent
│     │  ├─ DemoAgent.java         无密钥时的规则引擎（复用同一套 ShopTools）
│     │  ├─ ChatStream.java        SSE 事件封装
│     │  ├─ ChatStreamRegistry.java 会话 → SSE 通道
│     │  └─ ToolTraceEmitter.java  工具执行中把轨迹推给前端
│     ├─ repository/               ProductRepository / OrderRepository（JdbcTemplate）
│     ├─ model/                    Product / Order / OrderItem / TrackingNode / AfterSaleTicket
│     ├─ service/ChatService.java  对话编排 + 线程池
│     └─ web/                      ChatController / CatalogController / MetaController
└─ web/                            React 前端
   └─ src/
      ├─ api/                      fetch 封装 + SSE 解析
      ├─ hooks/useChat.ts          会话状态、localStorage 持久化
      ├─ components/               消息、商品卡、订单卡、工具轨迹面板…
      └─ pages 内嵌于 App.tsx      对话 / 商品库 / 我的订单
```

## 快速开始

### 1. 启动后端

```bash
cd server
mvn spring-boot:run
```

默认端口 `8080`，H2 内存库启动时自动执行 `schema.sql` + `data.sql`。
`http://localhost:8080/h2-console` 可查看数据（JDBC URL：`jdbc:h2:mem:shopmate`，用户 `SA`，密码留空）。

打包运行：

```bash
mvn -DskipTests package
java -jar target/shopmate-server-1.0.0.jar
```

### 2. 启动前端

```bash
cd web
npm install
npm run dev          # http://127.0.0.1:5173
```

Vite 已把 `/api` 代理到 `http://127.0.0.1:8080`，无需额外跨域配置。

### 3. 接入真实大模型（可选）

不配置密钥时项目以 **演示模式（demo / rule-engine）** 运行，功能完整。
接入大模型只需设置环境变量后重启后端：

```bash
# 阿里云百炼（默认 qwen-plus）
set SHOPMATE_LLM_API_KEY=sk-xxxx
set SHOPMATE_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
set SHOPMATE_LLM_MODEL=qwen-plus

# DeepSeek
set SHOPMATE_LLM_BASE_URL=https://api.deepseek.com/v1
set SHOPMATE_LLM_MODEL=deepseek-chat

# 本地 Ollama
set SHOPMATE_LLM_BASE_URL=http://127.0.0.1:11434/v1
set SHOPMATE_LLM_MODEL=qwen2.5:7b
set SHOPMATE_LLM_API_KEY=ollama
```

对应的 `application.yml` 配置项：

```yaml
shopmate:
  llm:
    base-url: ${SHOPMATE_LLM_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
    api-key: ${SHOPMATE_LLM_API_KEY:}
    model: ${SHOPMATE_LLM_MODEL:qwen-plus}
    temperature: 0.3
```

启动日志出现 `未配置 shopmate.llm.api-key，已启用内置规则引擎` 即表示处于演示模式；
顶部导航的模式徽标会显示 `LLM Agent · qwen-plus` 或 `演示模式 · rule-engine`。

## Agent 设计

```
用户输入
   │  POST /api/chat  (sessionId + message)
   ▼
ChatService ──► AiServices<ShopAssistant>  ← MessageWindowChatMemory（按 sessionId 保留 20 条）
                     │  LLM 决定调用哪个工具
                     ▼
                 ShopTools（@Tool + @ToolMemoryId）
                     │  ① ToolTraceEmitter.begin → SSE tool 事件（running）
                     │  ② 查询 H2（商品 / 订单 / 物流 / 工单）
                     │  ③ done(payload) → SSE tool 事件（done + 结构化数据）
                     ▼
              onPartialResponse → SSE token 事件（逐字）
                     ▼
                完成 → SSE done → 前端渲染卡片
```

工具清单（[ShopTools.java](server/src/main/java/com/shopmate/agent/agent/ShopTools.java)）：

| 工具 | 参数 | 返回 |
| --- | --- | --- |
| `searchProducts` | keyword, category, minPrice, maxPrice | 商品列表 |
| `getProductDetail` | productId | 单件商品 |
| `queryOrders` | status | 订单列表 |
| `getOrderDetail` | orderNo | 订单 + 明细 |
| `checkLogistics` | orderNo | 订单 + 轨迹 |
| `createAfterSaleTicket` | orderNo, type, reason | 售后工单 |

工具内部通过 `@ToolMemoryId` 拿到 `sessionId`，再从 `ChatStreamRegistry` 找到对应的 SSE 通道——
这样工具在任意线程执行都能把轨迹推回正确的会话。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/meta` | 运行模式、模型名、演示用户、建议问题、品类、商品/订单数量 |
| GET | `/api/products?keyword=&category=` | 商品检索 |
| GET | `/api/products/{id}` | 商品详情 |
| GET | `/api/orders?status=ALL` | 订单列表（SHIPPED / DELIVERED / PAID / COMPLETED） |
| GET | `/api/orders/{orderNo}` | 订单详情 |
| POST | `/api/orders/{orderNo}/after-sale` | 创建售后工单 `{type, reason}` |
| POST | `/api/chat` | 对话，`text/event-stream`，body `{sessionId, message}` |

### SSE 事件

| event | data |
| --- | --- |
| `meta` | `{sessionId, mode, model}` |
| `tool` | `{id, name, label, status, args?, summary?, payload?}`，`status=running/done/fail` |
| `token` | `{text}` |
| `error` | `{message}` |
| `done` | `{sessionId}` |

`payload.type` 决定前端渲染哪种卡片：`products` / `orders` / `ticket`。

## 演示数据

- 演示用户：`U1001` 林晓（`ShopTools.DEMO_USER_ID`，接入真实登录后可替换为当前用户）
- 12 件商品，覆盖数码 / 家电 / 美妆 / 食品
- 4 笔订单：`SM20261105001` 已发货、`SM20261028002` 已签收、`SM20261110003` 待发货、`SM20260912004` 已完成
- 仅「已签收 / 已完成」的订单可以创建售后工单，同一订单防重复建单

可以试试这些问题：

1. 预算 3000 元以内，推荐一款拍照好的手机
2. 我的订单到哪了
3. SM20261105001 的物流到哪了
4. 我想退掉 SM20261028002 这单，扫地机器人噪音太大
5. 降噪耳机有什么推荐

## 说明

- 数据存放在 H2 内存库，重启即重置；换成 MySQL 只需替换 `spring.datasource` 配置。
- 会话在浏览器端用 `localStorage` 持久化，服务端按 `sessionId` 维护对话记忆，刷新页面不丢上下文。
- 本项目为学习 / 面试演示用途，未包含真实支付、鉴权与风控。