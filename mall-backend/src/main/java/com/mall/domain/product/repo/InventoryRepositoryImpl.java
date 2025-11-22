package com.mall.domain.product.repo;

import com.mall.common.exception.BizException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepositoryCustom {

    private final EntityManager em;

    @Transactional
    @Override
    public boolean tryReserve(Long productId, int qty) {
        String jpql = """
            UPDATE Inventory i
               SET i.availableQty = i.availableQty - :q,
                   i.updatedAt = :now
             WHERE i.productId = :pid
               AND i.availableQty >= :q
        """;
        int n = em.createQuery(jpql)
                .setParameter("q", qty)
                .setParameter("pid", productId)
                .setParameter("now", Instant.now())
                .executeUpdate();
        return n == 1;
    }

    @Transactional
    @Override
    public void release(Long productId, int qty) {
        em.createQuery("""
            UPDATE Inventory i
               SET i.availableQty = i.availableQty + :q,
                   i.updatedAt = :now
             WHERE i.productId = :pid
        """)
        .setParameter("q", qty)
        .setParameter("pid", productId)
        .setParameter("now", Instant.now())
        .executeUpdate();
    }

    @Transactional
    @Override
    public void deductStock(Long productId, int qty) {
        int n = em.createQuery("""
            UPDATE Inventory i
               SET i.stockQty = i.stockQty - :q,
                   i.updatedAt = :now
             WHERE i.productId = :pid
               AND i.stockQty >= :q
        """)
        .setParameter("q", qty)
        .setParameter("pid", productId)
        .setParameter("now", Instant.now())
        .executeUpdate();
        if (n != 1) {
            throw new BizException("STOCK_DEDUCT_FAILED");
        }
    }
}
