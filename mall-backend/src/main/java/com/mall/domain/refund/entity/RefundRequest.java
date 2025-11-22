package com.mall.domain.refund.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="refund_request")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundRequest {

    public enum Status {
        APPLIED,
        APPROVED,
        REJECTED,
        REFUNDED,
        SUCCESS,   // ⬅️ 平台退款成功（必须有，服务里用到）
        FAILED     // （可选）退款失败/关闭
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long registerId;

    @Column(nullable=false, unique=true, length=64)
    private String refundNo;

    private String externalRefundNo;

    @Column(name = "channel", length = 32)
    private String channel;            // ⬅️ 渠道 ALIPAY/WECHAT/...

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private Status status = Status.APPLIED;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal refundAmount;

    @Column(precision=12, scale=2)
    private BigDecimal approvedAmount;

    private Long handledBy; // sys_user.id
    @Column(columnDefinition = "TEXT")
    private String handledRemark;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
