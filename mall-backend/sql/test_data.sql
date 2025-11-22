-- =========================================
-- Mall DB 测试数据构造脚本（可重放）
-- PostgreSQL 14+ 适用
-- 说明：
-- 1) 显式指定 id，便于外键引用与重复执行（配合 ON CONFLICT）
-- 2) 金额字段单位：元；时区：TIMESTAMPTZ 使用 NOW()
-- 3) 覆盖场景：SALE 商品购买 + 支付成功 + 退款回补；RENT_ONLY 商品在架
-- =========================================

BEGIN;

-- ---------- 1. 系统用户 / 前台用户 ----------
INSERT INTO sys_user (id, username, password_hash, real_name, gender, role, is_active, created_at, updated_at)
VALUES
  (1, 'admin',  '$2a$10$qbZX8NbV02ZMGDcVzYDjfe4.0aPE2fx/H/3QHMmRLz8SgwSFaJBYG', '超级管理员', 'MALE', 'SUPER_ADMIN', TRUE, NOW(), NOW()),
  (2, 'op01',   '$2a$10$qbZX8NbV02ZMGDcVzYDjfe4.0aPE2fx/H/3QHMmRLz8SgwSFaJBYG',    '运营一号',   'FEMALE','OPERATOR',    TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_user (id, phone, email, password_hash, nickname, gender, wx_openid, is_active, created_at, updated_at)
VALUES
  (1001, '13800138000', 'u1@example.com', '{bcrypt}$2a$10$demo_u1', '雨落森', 'MALE',   'wx_openid_u1', TRUE, NOW(), NOW()),
  (1002, '13900139000', 'u2@example.com', '{bcrypt}$2a$10$demo_u2', '清风',   'FEMALE', 'wx_openid_u2', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 2. 商品分类（含父子） ----------
INSERT INTO product_category (id, parent_id, category_number, category_name, category_icon, category_desc, quantity_unit, display, sort_order, is_active, created_at, updated_at)
VALUES
  (10, NULL, 'CATE-ROOT', '根分类',  'icon-root', '顶级分类', '件', TRUE, 0, TRUE, NOW(), NOW()),
  (11, 10,   'CATE-FLW',  '花束',    'icon-flw',  '鲜花与花束', '束', TRUE, 10, TRUE, NOW(), NOW()),
  (12, 10,   'CATE-ACC',  '配件',    'icon-acc',  '配件耗材',   '件', TRUE, 20, TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 3. 地址码（与分类关联） ----------
INSERT INTO address_code (id, code, name, category, category_id, img_url, related_pages, qr_code_value, is_active, created_by, created_at, updated_at)
VALUES
  (2001, 'ADDR-001', '前台A区', '现场', 11, NULL, '/pages/a', 'QR-ADDR-001', TRUE, 1, NOW(), NOW()),
  (2002, 'ADDR-002', '前台B区', '现场', 12, NULL, '/pages/b', 'QR-ADDR-002', TRUE, 2, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 4. 登记（依赖 sys_user + address_code） ----------
INSERT INTO register (id, register_time, name, identity_id, gender, related_name, related_mobile, address_id, address_name, address_status, is_active, created_by, created_at, updated_at)
VALUES
  (3001, NOW(), '张三', 'ID-330102-0001', 'MALE',   '李四', '18800000001', 2001, '前台A区', 'YES', TRUE, 1, NOW(), NOW()),
  (3002, NOW(), '王五', 'ID-330102-0002', 'FEMALE', '赵六', '18800000002', 2002, '前台B区', 'NO',  TRUE, 2, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 5. 商品（SALE / RENT_ONLY） ----------
INSERT INTO product (id, product_number, category_id, product_name, product_subtitle, description_text, wl_text, main_image_url,
                     sale_mode, price_sale, price_unit, price_rent_per_day, deposit_amount, is_on_shelf, sort_product, created_at, updated_at)
VALUES
  -- SALE 商品：经典花束（可直接购买）
  (5001, 'P-FLW-001', 11, '经典红玫瑰花束', '情人节/纪念日首选', '19枝红玫瑰，含配草与包装', '悼念词文案可选', '/imgs/flw001.jpg',
   'SALE', 199.00, '束', NULL, NULL, TRUE, 10, NOW(), NOW()),
  -- RENT_ONLY 商品：拱门花柱（仅租赁）
  (5002, 'P-ACC-777', 12, '户外花门（拱门）', '户外典礼/婚礼', '户外拱门+两侧花柱', NULL, '/imgs/acc777.jpg',
   'RENT_ONLY', NULL, '套', 299.00, 500.00, TRUE, 20, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 6. 商品图片 ----------
INSERT INTO product_image (id, product_id, image_url, sort_order, created_at)
VALUES
  (6001, 5001, '/imgs/flw001_1.jpg', 1, NOW()),
  (6002, 5001, '/imgs/flw001_2.jpg', 2, NOW()),
  (6003, 5002, '/imgs/acc777_1.jpg', 1, NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 7. 库存（SALE 用 stock_qty；RENT_ONLY 用 available_qty） ----------
INSERT INTO inventory (product_id, stock_qty, available_qty, updated_at)
VALUES
  (5001, 100, 0, NOW()),   -- 可售 100 束
  (5002, 0,   5, NOW())    -- 可租 5 套
ON CONFLICT (product_id) DO NOTHING;

-- ---------- 8. 订单（用户 1001 在登记点 3001 下单购买 SALE 商品） ----------
-- 订单金额：unit 199 * qty 2 => total_amount 398，实付 398
INSERT INTO "order" (id, user_id, order_no, status, total_amount, payable_amount, currency_code,
                     contact_name, contact_phone, register_id, register_name, shipping_address,
                     remark, need_wl, created_at, updated_at, pay_at)
VALUES
  (7001, 1001, 'ORD-20251107-0001', 'PAID', 398.00, 398.00, 'CNY',
   '张三', '13800138000', 3001, '前台A区', '上海市静安区XXX路123号',
   '请在今天下午前送达', TRUE, NOW(), NOW(), NOW())  -- 已支付订单，pay_at 有值
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE IF NOT EXISTS _exists_order_7001 AS
SELECT 1 AS x FROM "order" WHERE id = 7001;

-- ---------- 9. 订单明细 ----------
INSERT INTO order_item (id, order_id, product_id, product_name_snap, sale_mode_snap, unit_price, quantity, rent_days, line_amount, created_at)
SELECT 8001, 7001, 5001, '经典红玫瑰花束', 'SALE', 199.00, 2, NULL, 398.00, NOW()
WHERE EXISTS (SELECT 1 FROM _exists_order_7001)
ON CONFLICT (id) DO NOTHING;

-- ---------- 10. 支付（成功） ----------
INSERT INTO payment (id, order_id, payment_no, method, status, amount, paid_at, raw_payload, created_at, updated_at, external_trade_no)
SELECT 9001, 7001, 'PAY-20251107-0001', 'ALI_PAY', 'SUCCESS', 398.00, NOW(),
       '{"channel":"alipay","trade_status":"TRADE_SUCCESS"}'::jsonb, NOW(), NOW(), 'ALI-TRADE-0001'
WHERE EXISTS (SELECT 1 FROM _exists_order_7001)
ON CONFLICT (id) DO NOTHING;

-- 确保唯一索引 (order_id WHERE status='SUCCESS') 可被命中
-- 若之前已经有 SUCCESS，则这条会 ON CONFLICT(id) 拦住；本脚本不做状态切换

-- ---------- 11. 库存账本：扣减（与订单挂钩） ----------
-- 业务含义：支付成功后，将 SALE 商品库存扣减（-2）
-- 注意：inventory_txn.UNIQUE (biz_type, biz_id, product_id)
INSERT INTO inventory_txn (id, product_id, delta_qty, biz_type, biz_id, created_at)
VALUES
  (9101, 5001, -2, 'PAY_DEDUCT', '7001', NOW())
ON CONFLICT (id) DO NOTHING;

-- 同步更新库存（仅测试脚本用；正式环境应由服务端事务完成）
UPDATE inventory SET stock_qty = GREATEST(stock_qty - 2, 0), updated_at = NOW()
WHERE product_id = 5001;

-- ---------- 12. 退款申请（用户申请整单行退款，运营审批同意并完成退款） ----------
-- 先插入 APPLIED
INSERT INTO refund_request (id, order_id, order_item_id, user_id, register_id, refund_no, channel,
                            external_refund_no, reason, status, refund_amount, approved_amount,
                            handled_by, handled_remark, created_at, updated_at, processed_at)
VALUES
  (10001, 7001, 8001, 1001, 3001, 'REF-20251107-0001', 'ALI_PAY',
   'ALI-REF-0001', '不小心下错单', 'REFUNDED', 398.00, 398.00,
   2, '同意退款', NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ---------- 13. 库存账本：回补（与退款挂钩） ----------
INSERT INTO inventory_txn (id, product_id, delta_qty, biz_type, biz_id, created_at)
VALUES
  (9102, 5001, 2, 'REFUND_RESTORE', 'REF-20251107-0001', NOW())
ON CONFLICT (id) DO NOTHING;

-- 同步回补库存（仅测试脚本用）
UPDATE inventory SET stock_qty = stock_qty + 2, updated_at = NOW()
WHERE product_id = 5001;

-- ---------- 14. 幂等键样例 ----------
INSERT INTO http_idempotency (scope, idem_key, req_hash, status, result_json, created_at, updated_at)
VALUES
  ('ORDER_CREATE', 'idem-ord-0001', 'sha256:deadbeef', 'DONE',  '{"order_no":"ORD-20251107-0001"}', NOW(), NOW()),
  ('PAY_START',    'idem-pay-0001', 'sha256:beadfeed', 'DONE',  '{"payment_no":"PAY-20251107-0001"}', NOW(), NOW())
ON CONFLICT DO NOTHING;

DROP TABLE IF EXISTS _exists_order_7001;

COMMIT;

-- =========================================
-- 验证查询（可选）
-- SELECT * FROM "order" WHERE id=7001;
-- SELECT * FROM order_item WHERE order_id=7001;
-- SELECT * FROM payment WHERE order_id=7001;
-- SELECT * FROM inventory WHERE product_id IN (5001,5002);
-- SELECT * FROM inventory_txn ORDER BY id;
-- SELECT * FROM refund_request WHERE id=10001;
