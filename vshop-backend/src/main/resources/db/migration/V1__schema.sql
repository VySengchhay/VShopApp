-- ============================================================
-- VShop schema
-- ============================================================

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(80) NOT NULL UNIQUE,
    icon       VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT        NOT NULL REFERENCES categories(id),
    name        VARCHAR(150)  NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL CHECK (price > 0),
    image_url   TEXT,
    stock       INT           NOT NULL DEFAULT 0 CHECK (stock >= 0),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products(category_id);

CREATE TABLE addresses (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    province   VARCHAR(80)  NOT NULL,
    district   VARCHAR(80)  NOT NULL,   -- Khan / Srok
    commune    VARCHAR(80)  NOT NULL,   -- Sangkat / Khum
    street     VARCHAR(200) NOT NULL,
    note       VARCHAR(200),
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

-- status: PENDING_PAYMENT, PAID, SHIPPING, DELIVERED, CANCELLED
CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    order_no       VARCHAR(20)   NOT NULL UNIQUE,
    user_id        BIGINT        NOT NULL REFERENCES users(id),
    status         VARCHAR(20)   NOT NULL,
    subtotal       NUMERIC(12,2) NOT NULL,
    delivery_fee   NUMERIC(12,2) NOT NULL,
    total          NUMERIC(12,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    -- Snapshot of the address at order time (address can change later)
    ship_full_name VARCHAR(100)  NOT NULL,
    ship_phone     VARCHAR(20)   NOT NULL,
    ship_address   TEXT          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    paid_at        TIMESTAMPTZ
);
CREATE INDEX idx_orders_user ON orders(user_id);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    product_name VARCHAR(150)  NOT NULL,   -- snapshot
    image_url    TEXT,                     -- snapshot
    unit_price   NUMERIC(12,2) NOT NULL,   -- snapshot: price can change later
    quantity     INT           NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- One order can have several payment attempts (expired QR -> new attempt).
-- status: PENDING, APPROVED, DECLINED, CANCELLED, EXPIRED, FAILED
CREATE TABLE payments (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT        NOT NULL REFERENCES orders(id),
    tran_id          VARCHAR(20)   NOT NULL UNIQUE,   -- our ID sent to PayWay (max 20 chars)
    payment_option   VARCHAR(30)   NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    apv              VARCHAR(50),                     -- approval code from PayWay
    qr_string        TEXT,
    abapay_deeplink  TEXT,
    checkout_qr_url  TEXT,
    expires_at       TIMESTAMPTZ   NOT NULL,
    request_payload  TEXT,    -- what we sent (without the hash secret)
    response_payload TEXT,    -- what PayWay answered
    callback_payload TEXT,    -- last callback PayWay pushed to us
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    paid_at          TIMESTAMPTZ
);
CREATE INDEX idx_payments_order ON payments(order_id);
