package com.mall.domain.refund.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdminRefundQueryRepository extends JpaRepository<com.mall.domain.refund.entity.RefundRequest, Long> {

    @Query(value = """
          SELECT rr.id                                           AS id,
                 rr.refund_no                                    AS refundNo,
                 COALESCE(r.address_name, ac.name, o.register_name) AS addressCodeName,
                 rr.processed_at                                 AS refundAt,
                 (rr.refund_amount)::text                        AS refundAmount,
                 o.order_no                                      AS orderNo,
                 ac.name                                         AS shopName,
                 o.contact_name                                  AS contactName,
                 o.contact_phone                                 AS contactPhone,
                 ac.img_url                                      AS shopImage,
                 rr.reason                                       AS reason,
                 (rr.status)::text                               AS status
            FROM refund_request rr
            LEFT JOIN "order"   o  ON o.id = rr.order_id
            LEFT JOIN "register" r  ON r.id = rr.register_id
            LEFT JOIN address_code ac ON ac.id = r.address_id
           WHERE 1=1
             AND (:refundNo IS NULL OR rr.refund_no ILIKE CONCAT('%', :refundNo, '%'))
             AND (:orderNo  IS NULL OR o.order_no  ILIKE CONCAT('%', :orderNo,  '%'))
             AND (:contactName  IS NULL OR o.contact_name  ILIKE CONCAT('%', :contactName,  '%'))
             AND (:contactPhone IS NULL OR o.contact_phone ILIKE CONCAT('%', :contactPhone, '%'))
             AND (:addressCodeName IS NULL OR COALESCE(r.address_name, ac.name, o.register_name) ILIKE CONCAT('%', :addressCodeName, '%'))
             AND (:startRefundAt IS NULL OR rr.processed_at >= CAST(:startRefundAt AS timestamptz))
             AND (:endRefundAt   IS NULL OR rr.processed_at <  CAST(:endRefundAt   AS timestamptz))
             AND (
                   :statusesCsv IS NULL OR :statusesCsv = ''
                   OR rr.status::text = ANY(
                         regexp_split_to_array(
                           regexp_replace(upper(:statusesCsv), '\\s+', '', 'g'),
                           ','
                         )
                   )
                 )
          """,
          countQuery = """
          SELECT count(1)
            FROM refund_request rr
            LEFT JOIN "order"   o  ON o.id = rr.order_id
            LEFT JOIN "register" r  ON r.id = rr.register_id
            LEFT JOIN address_code ac ON ac.id = r.address_id
           WHERE 1=1
             AND (:refundNo IS NULL OR rr.refund_no ILIKE CONCAT('%', :refundNo, '%'))
             AND (:orderNo  IS NULL OR o.order_no  ILIKE CONCAT('%', :orderNo,  '%'))
             AND (:contactName  IS NULL OR o.contact_name  ILIKE CONCAT('%', :contactName,  '%'))
             AND (:contactPhone IS NULL OR o.contact_phone ILIKE CONCAT('%', :contactPhone, '%'))
             AND (:addressCodeName IS NULL OR COALESCE(r.address_name, ac.name, o.register_name) ILIKE CONCAT('%', :addressCodeName, '%'))
             AND (:startRefundAt IS NULL OR rr.processed_at >= CAST(:startRefundAt AS timestamptz))
             AND (:endRefundAt   IS NULL OR rr.processed_at <  CAST(:endRefundAt   AS timestamptz))
             AND (
                     :statusesCsv IS NULL OR :statusesCsv = ''
                     OR rr.status::text = ANY(
                           regexp_split_to_array(
                             regexp_replace(upper(:statusesCsv), '\\s+', '', 'g'),
                             ','
                           )
                     )
                 )
          """,
          nativeQuery = true
    )
    Page<AdminRefundPageRow> pageRefunds(
            @Param("refundNo") String refundNo,
            @Param("addressCodeName") String addressCodeName,
            @Param("startRefundAt") String startRefundAt,
            @Param("endRefundAt")   String endRefundAt,
            @Param("orderNo") String orderNo,
            @Param("contactName") String contactName,
            @Param("contactPhone") String contactPhone,
            @Param("statusesCsv") String statusesCsv,
            Pageable pageable
    );

}
