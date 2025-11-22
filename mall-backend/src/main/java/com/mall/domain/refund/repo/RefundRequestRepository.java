package com.mall.domain.refund.repo;

import com.mall.domain.refund.entity.RefundRequest;
import com.mall.dto.refund.RefundRequestSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    // 你原本就应该有的：
    List<RefundRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    // ✳ 新增1：按退款单号查详情（审批、查看详情会用到）
    Optional<RefundRequest> findByRefundNo(String refundNo);

    // ✳ 新增2：列出待处理的（APPLIED）退款申请
    List<RefundRequest> findAllByStatusOrderByCreatedAtDesc(RefundRequest.Status status);

    @Query(value = "select * from refund_request where refund_no=:rn for update", nativeQuery = true)
    Optional<RefundRequest> findByRefundNoForUpdate(@Param("rn") String refundNo);


    Optional<RefundRequest> findByExternalRefundNo(String externalRefundNo);

    @Query(value = "select * from refund_request where external_refund_no=:no for update", nativeQuery = true)
    Optional<RefundRequest> findByExternalRefundNoForUpdate(@Param("no") String no);

    boolean existsByExternalRefundNo(String externalRefundNo);

    @Query(
            value = """
            SELECT
                r.refund_no      AS "refundNo",
                r.status         AS "status",
                r.refund_amount  AS "refundAmount",
                r.created_at     AS "createdAt",
                p.id             AS "productId",
                p.product_name   AS "productName",
                COALESCE(oi.quantity, 0) AS "quantity",
                p.main_image_url AS "mainImageUrl"
            FROM refund_request r
            LEFT JOIN order_item oi ON oi.id = r.order_item_id
            LEFT JOIN product    p  ON p.id = oi.product_id
            /* 如果是“我的退款列表”，加上 user_id 过滤 */
            WHERE (:userId IS NULL OR r.user_id = :userId)
            ORDER BY r.id DESC
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM refund_request r
            WHERE (:userId IS NULL OR r.user_id = :userId)
            """,
            nativeQuery = true
    )
    Page<RefundRequestSummaryRow> pageRefundSummaries(@Param("userId") Long userId,
                                                      Pageable pageable);
}
