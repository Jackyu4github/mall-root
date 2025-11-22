package com.mall.domain.product.repo;

public interface InventoryTxnRepository {
    /**
     * 原子执行“记账 -> 调整库存”：
     * - 若 (biz_type,biz_id,product_id) 已存在：no-op 返回 0
     * - 若新插入成功：根据 delta 扣/回补库存（扣时需 stock_qty>=abs(delta)），成功返回 1
     */
    int applyInventoryTxn(long productId, int deltaQty, String bizType, String bizId);
}
