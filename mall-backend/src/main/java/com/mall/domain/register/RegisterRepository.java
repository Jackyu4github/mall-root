package com.mall.domain.register;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RegisterRepository extends JpaRepository<Register, Long>,
        JpaSpecificationExecutor<Register>{

   /* @Query(
            value = """
        SELECT
          r.id, r.register_time, r.name, r.identity_id, r.gender,
          r.related_name, r.related_mobile,
          r.address_id, r.address_name, r.address_status,
          r.is_active, r.created_by, r.created_at, r.updated_at
        FROM register r
        WHERE (
                CAST(:start AS timestamp) IS NULL
             OR r.register_time >= CAST(:start AS timestamp)
        )
          AND (
                CAST(:end AS timestamp) IS NULL
             OR r.register_time <  CAST(:end AS timestamp)
          )
          AND (
                CAST(:addrId AS bigint) IS NULL
             OR r.address_id = CAST(:addrId AS bigint)
          )
          AND (
                :addrSt IS NULL
             OR r.address_status = CAST(:#{#addrSt?.name()} AS register_address_enum)
          )
          AND (
                CAST(:act AS boolean) IS NULL
             OR r.is_active = CAST(:act AS boolean)
          )
          AND (
               COALESCE(CAST(:kw AS text), '') = ''
            OR r.name            ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.identity_id     ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.related_name    ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.related_mobile  ILIKE CONCAT('%', CAST(:kw AS text), '%')
          )
        ORDER BY r.register_time DESC
        """,
            countQuery = """
        SELECT COUNT(1)
        FROM register r
        WHERE (
                CAST(:start AS timestamp) IS NULL
             OR r.register_time >= CAST(:start AS timestamp)
        )
          AND (
                CAST(:end AS timestamp) IS NULL
             OR r.register_time <  CAST(:end AS timestamp)
          )
          AND (
                CAST(:addrId AS bigint) IS NULL
             OR r.address_id = CAST(:addrId AS bigint)
          )
          AND (
                :addrSt IS NULL
             OR r.address_status = CAST(:#{#addrSt?.name()} AS register_address_enum)
          )
          AND (
                CAST(:act AS boolean) IS NULL
             OR r.is_active = CAST(:act AS boolean)
          )
          AND (
               COALESCE(CAST(:kw AS text), '') = ''
            OR r.name            ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.identity_id     ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.related_name    ILIKE CONCAT('%', CAST(:kw AS text), '%')
            OR r.related_mobile  ILIKE CONCAT('%', CAST(:kw AS text), '%')
          )
        """,
            nativeQuery = true
    )
    Page<Register> pageFilter(@Param("start") Instant start,
                              @Param("end") Instant end,
                              @Param("addrId") Long addressId,
                              @Param("addrSt") RegisterAddressStatus addressStatus,
                              @Param("act") Boolean active,
                              @Param("kw") String keyword,
                              Pageable pageable);*/

    // 只查一条
    Optional<Register> findFirstByAddressIdAndAddressStatus(Long addressId, RegisterAddressStatus addressStatus);

    // 如果 addressStatus 在实体里是 String
    List<Register> findByAddressIdAndAddressStatus(Long addressId, String addressStatus);

}
