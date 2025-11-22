package com.mall.domain.product.repo;

public interface InventoryRepositoryCustom {
    boolean tryReserve(Long productId, int qty);  // available -= qty（available>=qty 才成功）
    void    release(Long productId, int qty);     // 取消/支付失败回滚：available += qty
    void    deductStock(Long productId, int qty); // 发货/完成后：stock -= qty（>=qty）
}
