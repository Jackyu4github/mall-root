package com.mall.domain.order.repo;

import com.mall.domain.order.entity.Order;
import com.mall.dto.order.api.OrderStatusSummaryRow;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    // === 新增：加 PESSIMISTIC_WRITE 行级锁（必须在 @Transactional 中调用）===
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    // === 新增 1：按 id 列表查询 ===
    List<Order> findByIdIn(Collection<Long> ids);


    // === 新增 2：按订单号列表查询 ===
    List<Order> findByOrderNoIn(Collection<String> orderNos);


    @Query(
            value = """
        SELECT
            o.id                 AS "id",
            o.order_no           AS "orderNo",
            o.status             AS "status",
            o.payable_amount     AS "payableAmount",
            o.shipping_address   AS "shippingAddress",
            o.created_at AS "createdAt",
            COALESCE(SUM(oi.quantity), 0) AS "quantity",
            COALESCE(
                string_agg(DISTINCT p.main_image_url, ','),
                ''
            ) AS "images"
        FROM "order" o
        LEFT JOIN order_item oi ON oi.order_id = o.id
        LEFT JOIN product    p  ON p.id = oi.product_id
        WHERE (:userId IS NULL OR o.user_id = :userId)
          /* 状态过滤：如果 statuses 为空/NULL 就不限制；否则用 ANY 过滤 */
          AND (
                :statuses IS NULL
                OR cardinality(:statuses) = 0
                OR o.status::text = ANY(:statuses)
              )
        GROUP BY o.id, o.order_no, o.status, o.payable_amount, o.shipping_address
        ORDER BY o.id DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT o.id)
        FROM "order" o
        WHERE (:userId IS NULL OR o.user_id = :userId)
          AND (
                :statuses IS NULL
                OR cardinality(:statuses) = 0
                OR o.status::text = ANY(:statuses)
              )
        """,
            nativeQuery = true
    )
    Page<OrderStatusSummaryRow> pageOrderSummaries(
            @Param("userId") Long userId,
            @Param("statuses") String[] statuses,
            Pageable pageable
    );


    @Query(value = """
    SELECT DISTINCT o.register_id AS registerId,
                    r.name        AS registerName
    FROM "order" o
    JOIN register r ON r.id = o.register_id
    WHERE o.user_id = :userId
    ORDER BY o.register_id
    """, nativeQuery = true)
    List<UserRegisterOnly> findUserRegistersOnly(@Param("userId") Long userId);

}
