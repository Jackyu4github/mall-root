package com.mall.domain.refund.repo;

import java.time.Instant;

public interface AdminRefundPageRow {
    Long getId();
    String getRefundNo();
    String getAddressCodeName();
    Instant getRefundAt();
    String getRefundAmount();   // NUMERIC -> text via SQL::text，直接映射为 String
    String getOrderNo();
    String getShopName();
    String getContactName();
    String getContactPhone();
    String getShopImage();
    String getReason();
    String getStatus();         // enum::text
}
