DO $$
DECLARE
  v_category_id   BIGINT;
  v_addr_id       BIGINT;
  v_user_id       BIGINT;
  v_register_id   BIGINT;
  v_product_id    BIGINT;
  v_order_id      BIGINT;

  v_order_no   TEXT := 'ORD-20251110-TEST01';
  v_refund_no  TEXT := 'REF-20251110-TEST01';
BEGIN
  -- 1) 商品类目（address_code 需要 category_id）
  INSERT INTO product_category (category_number, category_name, category_icon, quantity_unit, display, is_active)
  VALUES ('C-TEST', '测试类目', 'icon-test', '件', TRUE, TRUE)
  ON CONFLICT DO NOTHING;

  SELECT id INTO v_category_id
  FROM product_category
  WHERE category_number = 'C-TEST'
  LIMIT 1;

  -- 2) 出售地点 address_code（register 需要）
  INSERT INTO address_code (code, name, category, category_id, img_url, related_pages, qr_code_value, is_active)
  VALUES ('AC-TEST-01', '测试地点A', '测试', v_category_id, 'http://example.com/a.jpg', 'page-a', 'QR-AC-TEST-01', TRUE)
  ON CONFLICT (code) DO UPDATE SET updated_at = NOW();

  SELECT id INTO v_addr_id
  FROM address_code
  WHERE code = 'AC-TEST-01';

  -- 3) 登记 register（order 需要 register_id）
  INSERT INTO "register" (register_time, name, identity_id, gender, related_name, related_mobile, address_id, address_name, address_status, is_active)
  VALUES (NOW(), '张三', 'ID-TEST-0001', 'MALE', '李四', '13900000001', v_addr_id, '测试地点A', 'YES', TRUE)
  ON CONFLICT (name) DO UPDATE SET updated_at = NOW();

  SELECT id INTO v_register_id
  FROM "register"
  WHERE name = '张三';

  -- 4) 前台用户 app_user（注意：没有 username 字段）
  INSERT INTO app_user (phone, email, password_hash, nickname, gender, is_active)
  VALUES ('13800000001', 'test1@example.com', 'hash-xxx', '测试用户1', 'MALE', TRUE)
  ON CONFLICT (phone) DO UPDATE SET updated_at = NOW();

  SELECT id INTO v_user_id
  FROM app_user
  WHERE phone = '13800000001';

  -- 5) 商品 + 库存
  INSERT INTO product (product_number, category_id, product_name, description_text, wl_text, main_image_url,
                       sale_mode, price_sale, price_unit, is_on_shelf)
  VALUES ('P-TEST-01', v_category_id, '测试商品1', '描述', '文案', 'http://example.com/p.jpg',
          'SALE', 199.00, '件', TRUE)
  ON CONFLICT DO NOTHING;

  SELECT id INTO v_product_id
  FROM product
  WHERE product_number = 'P-TEST-01';

  INSERT INTO inventory (product_id, stock_qty, available_qty)
  VALUES (v_product_id, 100, 0)
  ON CONFLICT (product_id) DO UPDATE SET updated_at = NOW();

  -- 6) 订单（PAID）
  INSERT INTO "order"(user_id, order_no, status, total_amount, payable_amount, currency_code,
                      contact_name, contact_phone, register_id, register_name, shipping_address, remark,
                      need_wl, created_at, updated_at, pay_at)
  VALUES (v_user_id, v_order_no, 'PAID'::order_status_enum, 199.00, 199.00, 'CNY',
          '张三', '13800000001', v_register_id, '张三', '上海市静安区XXX路123号', '测试订单',
          TRUE, NOW(), NOW(), NOW())
  ON CONFLICT (order_no) DO UPDATE SET updated_at = NOW();

  SELECT id INTO v_order_id
  FROM "order"
  WHERE order_no = v_order_no;

  -- 7) 订单明细（按你当前表结构字段：product_name_snap / sale_mode_snap / unit_price / quantity / rent_days / line_amount）
  INSERT INTO order_item (order_id, product_id, product_name_snap, sale_mode_snap, unit_price, quantity, rent_days, line_amount, created_at)
  VALUES (v_order_id, v_product_id, '测试商品1', 'SALE', 199.00, 1, NULL, 199.00, NOW())
  ON CONFLICT DO NOTHING;

  -- 8) 退款申请（APPLIED）
  INSERT INTO refund_request (order_id, order_item_id, user_id, register_id,
                              refund_no, channel, external_refund_no, reason, status,
                              refund_amount, approved_amount, handled_by, handled_remark,
                              created_at, updated_at, processed_at)
  VALUES (
    v_order_id,
    (SELECT id FROM order_item WHERE order_id = v_order_id LIMIT 1),
    v_user_id,
    v_register_id,
    v_refund_no,
    'ALI_PAY',
    NULL,
    '我不想要了（测试）',
    'APPLIED'::refund_status_enum,
    199.00,
    NULL,
    NULL,
    NULL,
    NOW(), NOW(),
    NULL
  )
  ON CONFLICT (refund_no) DO UPDATE SET updated_at = NOW();

END $$;
