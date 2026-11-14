DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS shop_order;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS order_tracking;
DROP TABLE IF EXISTS after_sale_ticket;
DROP TABLE IF EXISTS kb_article;
DROP TABLE IF EXISTS tool_call;

CREATE TABLE product (
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    brand          VARCHAR(60)  NOT NULL,
    category       VARCHAR(40)  NOT NULL,
    price          DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2),
    rating         DECIMAL(3, 2) NOT NULL,
    stock          INT NOT NULL,
    tags           VARCHAR(200),
    summary        VARCHAR(500)
);

CREATE TABLE shop_order (
    order_no     VARCHAR(40) PRIMARY KEY,
    user_id      VARCHAR(40) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    status_text  VARCHAR(20) NOT NULL,
    created_at   VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    receiver     VARCHAR(60),
    address      VARCHAR(200),
    carrier      VARCHAR(40),
    logistics_no VARCHAR(60)
);

CREATE TABLE order_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(40) NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    quantity     INT NOT NULL,
    price        DECIMAL(10, 2) NOT NULL
);

CREATE TABLE order_tracking (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    ts       VARCHAR(20) NOT NULL,
    text     VARCHAR(200) NOT NULL
);

CREATE TABLE after_sale_ticket (
    ticket_no  VARCHAR(40) PRIMARY KEY,
    order_no   VARCHAR(40) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    reason     VARCHAR(200) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at VARCHAR(20) NOT NULL
);

-- 知识库：退换货 / 物流 / 支付等平台规则，供检索增强使用
CREATE TABLE kb_article (
    id       BIGINT PRIMARY KEY,
    title    VARCHAR(120) NOT NULL,
    category VARCHAR(40)  NOT NULL,
    content  VARCHAR(1200) NOT NULL
);

-- 工具审计：每次工具调用落一行，记录入参、结果、耗时与状态
CREATE TABLE tool_call (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id    VARCHAR(40) NOT NULL UNIQUE,
    session_id  VARCHAR(64),
    tool_name   VARCHAR(60) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    args_json   VARCHAR(600),
    summary     VARCHAR(300),
    duration_ms BIGINT,
    created_at  VARCHAR(20) NOT NULL
);