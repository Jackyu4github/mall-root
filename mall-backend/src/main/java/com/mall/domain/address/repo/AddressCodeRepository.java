package com.mall.domain.address.repo;

import com.mall.domain.address.entity.AddressCode;
import com.mall.domain.register.Register;
import com.mall.dto.address.AddressCodeGaoBieProjection;
import com.mall.dto.address.AddressCodeGaoBieResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AddressCodeRepository extends JpaRepository<AddressCode, Long> {

    Optional<AddressCode> findByQrCodeValue(String qrCodeValue);

    Optional<AddressCode> findByCode(String code);

    @Query("""
           select a from AddressCode a
           where (:kw is null or :kw = '' or
                 lower(a.code) like lower(concat('%', :kw, '%')) or
                 lower(a.name) like lower(concat('%', :kw, '%')) or
                 lower(a.category) like lower(concat('%', :kw, '%')))
             and (:cid is null or a.categoryId = :cid)
             and (:act is null or a.active = :act)
           """)
    Page<AddressCode> search(@Param("kw") String keyword,
                             @Param("cid") Long categoryId,
                             @Param("act") Boolean active,
                             Pageable pageable);

    @Query(
            value = """
        SELECT
          a.id,
          a.name,
          a.img_url AS "imgUrl",
          EXISTS (
            SELECT 1
              FROM register r
             WHERE r.address_id = a.id
               AND r.address_status <> 'NO' 
               AND r.is_active = TRUE
               AND r.register_time >= COALESCE(CAST(:startTime AS TIMESTAMPTZ), '-infinity'::timestamptz)
               AND r.register_time  < COALESCE(CAST(:endTime   AS TIMESTAMPTZ), 'infinity'::timestamptz)
          ) AS "userStatus",
          (
            SELECT r2.name
              FROM register r2
             WHERE r2.address_id = a.id
               AND r2.address_status <> 'NO' 
               AND r2.is_active = TRUE
               AND r2.register_time >= COALESCE(CAST(:startTime AS TIMESTAMPTZ), '-infinity'::timestamptz)
               AND r2.register_time  < COALESCE(CAST(:endTime   AS TIMESTAMPTZ), 'infinity'::timestamptz)
             ORDER BY r2.register_time DESC
             LIMIT 1
          ) AS "registerName"
        FROM address_code a
        WHERE a.category_id = :categoryId
        ORDER BY a.id
      """,
            countQuery = """
        SELECT COUNT(*)
          FROM address_code a
         WHERE a.category_id = :categoryId
      """,
            nativeQuery = true
    )
    Page<AddressCodeGaoBieProjection> pageGaoBieByCategoryId(
            @Param("categoryId") Long categoryId,
            @Param("startTime") Instant startTime,   // 可为 null
            @Param("endTime")   Instant endTime,     // 可为 null
            Pageable pageable
    );


}
