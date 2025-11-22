// src/main/java/com/mall/domain/product/repo/InventoryRepository.java
package com.mall.domain.product.repo;

import com.mall.domain.product.entity.Inventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 预占库存：available_qty >= :qty 才能成功，成功则把 available_qty 减掉 qty
     * 返回 1 表示成功，0 表示失败（库存不足或不存在）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE inventory
           SET available_qty = available_qty - :qty,
               updated_at    = now()
         WHERE product_id    = :pid
           AND available_qty >= :qty
        """, nativeQuery = true)
    int tryReserve(@Param("pid") Long productId, @Param("qty") int qty);

    /**
     * 释放预占（例如取消订单/支付失败）：把 available_qty 加回 qty
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE inventory
           SET available_qty = available_qty + :qty,
               updated_at    = now()
         WHERE product_id    = :pid
        """, nativeQuery = true)
    int release(@Param("pid") Long productId, @Param("qty") int qty);

    /**
     * 消耗实库存（支付成功后，从总库存扣减；available 已在预占阶段扣过，不再动）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE inventory
           SET stock_qty  = stock_qty - :qty,
               updated_at = now()
         WHERE product_id = :pid
           AND stock_qty  >= :qty
        """, nativeQuery = true)
    int consume(@Param("pid") Long productId, @Param("qty") int qty);
}
