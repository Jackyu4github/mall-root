// src/main/java/com/mall/domain/inventory/repo/InventoryTxnRepositoryImpl.java
package com.mall.domain.product.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class InventoryTxnRepositoryImpl implements InventoryTxnRepository {

    private final EntityManager em;

    @Override
    @Transactional
    public int applyInventoryTxn(long productId, int deltaQty, String bizType, String bizId) {
        final String sql = """
        WITH ins AS (
          INSERT INTO inventory_txn(product_id, delta_qty, biz_type, biz_id)
          VALUES (:pid, :dq, cast(:bt as inventory_biz_type_enum), :bid)
          ON CONFLICT (biz_type, biz_id, product_id) DO NOTHING
          RETURNING 1
        )
        UPDATE inventory i
           SET stock_qty = stock_qty + :dq
         WHERE i.product_id = :pid
           AND (
                (:dq >= 0)  -- 回补
                OR (i.stock_qty >= (:dq * -1))  -- 扣减检查
               )
           AND EXISTS (SELECT 1 FROM ins)
        """;
        Query q = em.createNativeQuery(sql)
                .setParameter("pid", productId)
                .setParameter("dq", deltaQty)
                .setParameter("bt", bizType)
                .setParameter("bid", bizId);
        return q.executeUpdate();
    }
}
