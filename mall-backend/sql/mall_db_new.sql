-- =========================================
-- Mall DB 基础结构（幂等、安全可重放）
-- PostgreSQL 14+ 亲测语法通过
-- =========================================

-- =========================================
-- 0. 枚举类型定义（使用 DO $$ 防重复创建）
--   说明：不要轻易 DROP TYPE（可能被列引用），
--        这里用 duplicate_object 捕获已存在场景
-- =========================================

-- 系统用户角色：超级管理员 / 普通运营
DO $$ BEGIN
  CREATE TYPE sys_user_role_enum AS ENUM ('SUPER_ADMIN', 'OPERATOR');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 商品售卖模式：可出售 / 只出租
DO $$ BEGIN
  CREATE TYPE product_sale_mode_enum AS ENUM ('SALE', 'RENT_ONLY');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 订单状态
DO $$ BEGIN
  CREATE TYPE order_status_enum AS ENUM (
    'CREATED',    -- 下单未支付
    'PAID',       -- 已支付
    'RECEIVED',   -- 已接单
    'SHIPPED',    -- 已发货 / 已出库
    'COMPLETED',  -- 已完成（收货/租赁结束确认）
    'CANCELED'    -- 已取消
  );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 支付状态
DO $$ BEGIN
  CREATE TYPE payment_status_enum AS ENUM (
    'PENDING',    -- 待支付或支付中
    'SUCCESS',    -- 支付成功
    'FAILED'      -- 支付失败
  );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 支付方式
DO $$ BEGIN
  CREATE TYPE payment_method_enum AS ENUM (
    'ALI_PAY',
    'WECHAT_PAY',
    'CREDIT_CARD',
    'CASH'
  );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 退款状态（保留 SUCCESS/FAILED，服务端会用到）
DO $$ BEGIN
  CREATE TYPE refund_status_enum AS ENUM (
    'APPLIED',    -- 用户已申请
    'APPROVED',   -- 审核通过，待原路退回
    'REJECTED',   -- 审核拒绝
    'REFUNDED',   -- 已退款完成
    'SUCCESS',    -- 平台退款成功（外部渠道状态）
    'FAILED'      -- 退款失败/关闭
  );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 人的性别：男 / 女
DO $$ BEGIN
  CREATE TYPE human_gender_enum AS ENUM ('MALE', 'FEMALE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 地点使用状态：YES / NO
DO $$ BEGIN
  CREATE TYPE register_address_enum AS ENUM ('YES', 'NO');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;


-- =========================================
-- 0'. 客户端幂等键记录表
-- =========================================
CREATE TABLE IF NOT EXISTS http_idempotency (
  scope        VARCHAR(64)   NOT NULL,                 -- 业务域：ORDER_CREATE / PAY_START / REFUND_APPLY 等
  idem_key     VARCHAR(128)  NOT NULL,                 -- 客户端传来的幂等键
  req_hash     VARCHAR(64),                            -- 可选：请求体哈希
  status       VARCHAR(16)   NOT NULL DEFAULT 'PENDING', -- PENDING / DONE / FAILED
  result_json  JSONB,                                  -- 可选：返回体缓存（适合同步接口）
  created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  PRIMARY KEY (scope, idem_key)
);


-- =========================================
-- 1. 系统用户（后台维护人员 / 运营账号）
-- =========================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) UNIQUE NOT NULL,      -- 登录名
    password_hash   TEXT        NOT NULL,             -- 密码哈希
    real_name       VARCHAR(64),                      -- 展示名/联系人姓名
    gender          VARCHAR(64),                -- 性别
    avatar_url      TEXT,
    role            sys_user_role_enum NOT NULL DEFAULT 'OPERATOR',
    push_token      VARCHAR(256),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role ON sys_user(role);


-- =========================================
-- 2. 商城前台用户（C 端）
-- =========================================
CREATE TABLE IF NOT EXISTS app_user (
    id              BIGSERIAL PRIMARY KEY,
    phone           VARCHAR(32),
    email           VARCHAR(128),
    password_hash   TEXT        NOT NULL,
    nickname        VARCHAR(64),
    gender          VARCHAR(64),                -- 性别
    wx_openid       VARCHAR(255),
    avatar_url      TEXT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (phone),
    UNIQUE (email)
);

CREATE UNIQUE INDEX uq_app_user_wx_openid ON app_user(wx_openid) WHERE wx_openid IS NOT NULL;


-- =========================================
-- 3. 商品分类
-- =========================================
CREATE TABLE IF NOT EXISTS product_category (
    id               BIGSERIAL PRIMARY KEY,
    parent_id        BIGINT REFERENCES product_category(id) ON DELETE SET NULL,
    category_number  VARCHAR(128) NOT NULL,
    category_name    VARCHAR(128) NOT NULL,
    category_icon    VARCHAR(128) NOT NULL,
    category_desc    TEXT,
    quantity_unit    VARCHAR(128) NOT NULL,
    display          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order       INT NOT NULL DEFAULT 0,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_category_parent ON product_category(parent_id);


-- =========================================
-- 12（前置）. 商品出售地点管理（address_code）
--   ✅ 修正笔误：name/category 列去掉错误的 "NOT"/"NO" 关键字
--   ✅ 保留 code/qr_code_value 唯一；补常用索引
-- =========================================
CREATE TABLE IF NOT EXISTS address_code (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(256) NOT NULL UNIQUE,    -- 编号
    name                VARCHAR(256),                    -- 名称（可空）
    category            VARCHAR(256),                    -- 类别（可空）
    category_id         BIGINT NOT NULL ,
    img_url             VARCHAR(256),                    -- 图片
    related_pages       BIGINT NOT NULL REFERENCES product_category(id) ON DELETE RESTRICT,                    -- 关联页面
    qr_code_value       VARCHAR(256) NOT NULL,    -- 扫码解析出来的码值/短码
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_address_code_category    ON address_code(category);
CREATE INDEX IF NOT EXISTS idx_address_code_category_id ON address_code(category_id);


-- =========================================
-- 13（前置）. 登记（register）
--   依赖 address_code，放在其后创建
-- =========================================
CREATE TABLE IF NOT EXISTS register (
    id                  BIGSERIAL PRIMARY KEY,
    register_time       TIMESTAMPTZ NOT NULL DEFAULT NOW(),   -- 登记时间
    name                VARCHAR(256) NOT NULL UNIQUE,         -- 姓名
    identity_id         VARCHAR(256) NOT NULL UNIQUE,         -- 身份证
    gender              VARCHAR(64),                    -- 性别
    related_name        VARCHAR(256) NOT NULL UNIQUE,         -- 关联人
    related_mobile      VARCHAR(256) NOT NULL UNIQUE,         -- 关联人手机
    address_id          BIGINT NOT NULL REFERENCES address_code(id) ON DELETE RESTRICT, -- 地点id
    address_name        VARCHAR(256),                         -- 地点名称
    address_status      VARCHAR(64) NOT NULL DEFAULT 'NO', -- 地点使用状态
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_register_time ON register(register_time);
CREATE INDEX IF NOT EXISTS idx_register_address_id ON register(address_id);


-- =========================================
-- 4. 商品主表
-- =========================================
CREATE TABLE IF NOT EXISTS product (
    id                  BIGSERIAL PRIMARY KEY,
    product_number      VARCHAR(128) NOT NULL,
    category_id         BIGINT NOT NULL REFERENCES product_category(id) ON DELETE RESTRICT,
    product_name        VARCHAR(256) NOT NULL,
    product_subtitle    VARCHAR(512),                -- 卖点/副标题
    description_text    TEXT,                        -- 富文本/图文描述
    wl_text             TEXT,                        -- ✅ 修正对齐：列名保留，类型 TEXT
    main_image_url      TEXT,                        -- 主图
    sale_mode           VARCHAR(64) NOT NULL DEFAULT 'SALE',
    price_sale          NUMERIC(12,2),
    price_unit          VARCHAR(128),
    price_rent_per_day  NUMERIC(12,2),
    deposit_amount      NUMERIC(12,2),
    is_on_shelf         BOOLEAN NOT NULL DEFAULT TRUE,
    is_wanlian          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_product        INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_category    ON product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_is_on_shelf ON product(is_on_shelf);


-- =========================================
-- 5. 商品图片（多图轮播）
-- =========================================
CREATE TABLE IF NOT EXISTS product_image (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    image_url       TEXT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_image_product ON product_image(product_id);


-- =========================================
-- 6. 库存管理
--   对 SALE 商品：stock_qty 表示可售库存
--   对 RENT_ONLY 商品：available_qty 表示可出租数量
-- =========================================
CREATE TABLE IF NOT EXISTS inventory (
    product_id      BIGINT PRIMARY KEY REFERENCES product(id) ON DELETE CASCADE,
    product_name    VARCHAR(256),
    stock_qty       INT NOT NULL DEFAULT 0,          -- 可售库存
    available_qty   INT NOT NULL DEFAULT 0,          -- 可出租剩余数量
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 非负检查约束（放在建表之后）
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    WHERE c.conname = 'ck_inventory_non_negative'
      AND c.conrelid = 'inventory'::regclass
  ) THEN
    ALTER TABLE inventory
      ADD CONSTRAINT ck_inventory_non_negative
      CHECK (stock_qty >= 0 AND available_qty >= 0);
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_inventory_stock ON inventory(stock_qty);


-- =========================================
-- 7（后置）. 订单主表  —— 现在 register 已存在
-- =========================================
CREATE TABLE IF NOT EXISTS "order" (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    order_no            VARCHAR(64) NOT NULL UNIQUE,   -- 业务订单号
    status              VARCHAR(64) NOT NULL DEFAULT 'CREATED',
    total_amount        NUMERIC(12,2) NOT NULL,        -- 商品总价（含押金等，未减优惠）
    payable_amount      NUMERIC(12,2) NOT NULL,        -- 实际应付
    currency_code       VARCHAR(8) NOT NULL DEFAULT 'CNY',
    contact_name        VARCHAR(64),
    contact_phone       VARCHAR(32),
    address_id          BIGINT,
    register_id         BIGINT NOT NULL REFERENCES "register"(id) ON DELETE CASCADE,
    register_name       VARCHAR(64),
    shipping_address    TEXT,
    remark              TEXT,
    need_wl             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pay_at              TIMESTAMPTZ                    -- ✅ 允许 NULL，支付成功时回填
);

CREATE INDEX IF NOT EXISTS idx_order_user   ON "order"(user_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON "order"(status);


-- =========================================
-- 8. 订单明细
-- =========================================
CREATE TABLE IF NOT EXISTS order_item (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    product_name_snap   VARCHAR(256) NOT NULL,       -- 下单时的商品名快照
    sale_mode_snap      VARCHAR(64) NOT NULL,
    unit_price          NUMERIC(12,2) NOT NULL,      -- 单价(购买价 或 日租价)
    quantity            INT NOT NULL DEFAULT 1,      -- 购买件数 / 租赁数量
    rent_days           INT,                         -- 租赁天数（租赁类）
    ext_field           JSONB,
    line_amount         NUMERIC(12,2) NOT NULL,      -- 行小计
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_item(order_id);


-- =========================================
-- 9. 支付记录
-- =========================================
CREATE TABLE IF NOT EXISTS payment (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    payment_no          VARCHAR(64) NOT NULL UNIQUE,      -- 支付流水号
    method              VARCHAR(64) NOT NULL,     -- 支付方式
    status              VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    amount              NUMERIC(12,2) NOT NULL,
    paid_at             TIMESTAMPTZ,                      -- 成功时间
    raw_payload         JSONB,                            -- 第三方回调原文
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 第三方交易号唯一（仅在非空时）
ALTER TABLE payment
  ADD COLUMN IF NOT EXISTS external_trade_no VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_external_trade_no
  ON payment (external_trade_no) WHERE external_trade_no IS NOT NULL;

-- 每个订单最多有一个 SUCCESS 的支付（任一渠道）
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_order_success_once
  ON payment (order_id)
  WHERE status = 'SUCCESS';

CREATE INDEX IF NOT EXISTS idx_payment_order   ON payment(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_status  ON payment(status);


-- =========================================
-- 10. 退款申请/处理
-- =========================================
CREATE TABLE IF NOT EXISTS refund_request (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES "order"(id) ON DELETE RESTRICT,
    order_item_id       BIGINT     REFERENCES order_item(id) ON DELETE SET NULL,
    user_id             BIGINT NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    register_id         BIGINT NOT NULL REFERENCES "register"(id) ON DELETE CASCADE,
    refund_no           VARCHAR(64) NOT NULL UNIQUE,  -- 退款单号
    channel             VARCHAR(32),
    external_refund_no  VARCHAR(128),
    reason              TEXT,                         -- 用户填写原因
    status              VARCHAR(64) NOT NULL DEFAULT 'APPLIED',
    refund_amount       NUMERIC(12,2) NOT NULL,       -- 申请退款金额
    approved_amount     NUMERIC(12,2),                -- 审批通过金额（可能小于申请）
    handled_by          BIGINT REFERENCES sys_user(id) ON DELETE SET NULL, -- 审核人(运营)
    handled_remark      TEXT,                         -- 审核意见/备注
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ                   -- 审核完成/退款完成时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_refund_request_external_no
  ON refund_request (external_refund_no)
  WHERE external_refund_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_refund_order   ON refund_request(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_user    ON refund_request(user_id);
CREATE INDEX IF NOT EXISTS idx_refund_status  ON refund_request(status);


-- =========================================
-- D. 库存幂等账本（对每个业务动作/商品只生效一次）
-- =========================================
DO $$ BEGIN
  CREATE TYPE inventory_biz_type_enum AS ENUM ('PAY_DEDUCT','REFUND_RESTORE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS inventory_txn (
  id           BIGSERIAL PRIMARY KEY,
  product_id   BIGINT NOT NULL,
  delta_qty    INTEGER NOT NULL,                 -- 负数=扣，正数=回补
  biz_type     inventory_biz_type_enum NOT NULL,
  biz_id       VARCHAR(128) NOT NULL,            -- 订单ID 或 退款单号
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (biz_type, biz_id, product_id)
);


-- =========================================
-- 11. 系统日志 / 操作审计
-- =========================================
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_type      VARCHAR(16) NOT NULL,             -- 'SYS' or 'USER'
    actor_id        BIGINT,                           -- sys_user.id / app_user.id
    action          VARCHAR(128) NOT NULL,            -- 操作名，如 "CREATE_PRODUCT"
    target_type     VARCHAR(64),                      -- 作用对象类型，如 "product", "order"
    target_id       BIGINT,                           -- 作用对象ID
    detail          TEXT,                             -- 额外信息(变更前后字段等) —— 改为 TEXT
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_actor      ON audit_log(actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_target     ON audit_log(target_type, target_id);

-- =========================================
-- 14. 兼容 JDBC/Hibernate 的枚举隐式转换（强烈建议保留）
-- 作用：允许 'SUCCESS' 这样的 varchar 字面量与原生 enum 列直接比较/写入
-- 典型症状：operator does not exist: character varying = payment_status_enum
-- 说明：WITH INOUT AS IMPLICIT 会创建 in/out 函数并设为隐式转换；duplicate_object 时忽略
-- =========================================

DO $$ BEGIN
  CREATE CAST (varchar AS human_gender_enum)       WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (human_gender_enum AS varchar)       WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS register_address_enum)   WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (register_address_enum AS varchar)   WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS product_sale_mode_enum)  WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (product_sale_mode_enum AS varchar)  WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS order_status_enum)       WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (order_status_enum AS varchar)       WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS payment_status_enum)     WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (payment_status_enum AS varchar)     WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS payment_method_enum)     WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (payment_method_enum AS varchar)     WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS refund_status_enum)      WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (refund_status_enum AS varchar)      WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE CAST (varchar AS sys_user_role_enum)      WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  CREATE CAST (sys_user_role_enum AS varchar)      WITH INOUT AS IMPLICIT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;



-- =========================================
-- 15. Inventory 乐观锁列（如实体有 @Version private Long v;）
-- 说明：按“新增(默认0) → 回填NULL → 置非空”的顺序做，全部幂等
-- 避免：ERROR: column "v" of relation "inventory" contains null values
-- =========================================

DO $$
BEGIN
  -- 1) 不存在则新增，带默认 0（历史行不为 NULL）
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'inventory' AND column_name = 'v'
  ) THEN
    ALTER TABLE inventory ADD COLUMN v BIGINT DEFAULT 0;
  END IF;

  -- 2) 回填可能存在的 NULL
  UPDATE inventory SET v = 0 WHERE v IS NULL;

  -- 3) 尝试置为 NOT NULL（如果已是 NOT NULL 或被锁，忽略）
  BEGIN
    ALTER TABLE inventory ALTER COLUMN v SET NOT NULL;
  EXCEPTION WHEN others THEN
    NULL;
  END;
END
$$;



