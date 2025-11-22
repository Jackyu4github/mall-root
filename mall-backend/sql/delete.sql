BEGIN;

-- 1. 先删依赖最深的表
DROP TABLE IF EXISTS refund_request;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS order_item;

-- 2. 再删被引用的主表
DROP TABLE IF EXISTS "order";

-- 3. 解除 product_category 的下游引用
DROP TABLE IF EXISTS register;         -- 引用 address_code
DROP TABLE IF EXISTS address_code;     -- 引用 product_category

-- 4. 与 product 相关的子表
DROP TABLE IF EXISTS product_image;
DROP TABLE IF EXISTS inventory;

-- 5. 删 product 再删其父类目
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;

-- 6. 其余独立或弱依赖表
DROP TABLE IF EXISTS inventory_txn;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS http_idempotency;

-- 7. 用户表（被上面表引用的已先删除）
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS sys_user;

COMMIT;
