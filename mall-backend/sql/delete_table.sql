-- =========================================
-- Mall DB 删除脚本（安全可重放）
-- 注意：
-- 1) 先删表（按外键逆序），再删 ENUM 类型
-- 2) 未使用 CASCADE，避免误删其它对象；若库内仍有依赖，可临时加 CASCADE
-- =========================================

BEGIN;

-- ---------- 表（按依赖逆序）----------

-- D. 库存幂等账本
DROP TABLE IF EXISTS inventory_txn;

-- 10. 退款申请/处理（依赖：order, order_item, app_user, register, sys_user）
DROP TABLE IF EXISTS refund_request;

-- 9. 支付记录（依赖：order）
DROP TABLE IF EXISTS payment;

-- 8. 订单明细（依赖：order, product）
DROP TABLE IF EXISTS order_item;

-- 7. 订单主表（依赖：app_user, register）
DROP TABLE IF EXISTS "order";

-- 6. 库存管理（依赖：product）
DROP TABLE IF EXISTS inventory;

-- 5. 商品图片（依赖：product）
DROP TABLE IF EXISTS product_image;

-- 4. 商品主表（依赖：product_category）
DROP TABLE IF EXISTS product;

-- 3. 商品分类（自引用）
DROP TABLE IF EXISTS product_category;

-- 13. 登记（依赖：address_code, sys_user）
DROP TABLE IF EXISTS register;

-- 12. 商品出售地点管理（依赖：product_category, sys_user）
DROP TABLE IF EXISTS address_code;

-- 11. 系统日志 / 操作审计（可引用 sys_user/app_user，但无 FK 强约束）
DROP TABLE IF EXISTS audit_log;

-- 2. 商城前台用户
DROP TABLE IF EXISTS app_user;

-- 1. 系统用户
DROP TABLE IF EXISTS sys_user;

-- 0'. 客户端幂等键记录表
DROP TABLE IF EXISTS http_idempotency;

-- ---------- ENUM 类型 ----------
-- （必须在所有引用它们的表删除后再删）

DROP TYPE IF EXISTS inventory_biz_type_enum;
DROP TYPE IF EXISTS register_address_enum;
DROP TYPE IF EXISTS human_gender_enum;
DROP TYPE IF EXISTS refund_status_enum;
DROP TYPE IF EXISTS payment_method_enum;
DROP TYPE IF EXISTS payment_status_enum;
DROP TYPE IF EXISTS order_status_enum;
DROP TYPE IF EXISTS product_sale_mode_enum;
DROP TYPE IF EXISTS sys_user_role_enum;

COMMIT;
