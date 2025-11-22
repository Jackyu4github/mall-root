package com.mall.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="payment")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    public enum Method {
        ALI_PAY,
        WECHAT_PAY,
        CREDIT_CARD,
        CASH
    }

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(nullable=false, unique=true, length=64)
    private String paymentNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private Method method = Method.CASH;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "payment_status_enum")
    private Status status = Status.PENDING;

    @Column(nullable=false, precision=18, scale=2)
    private BigDecimal amount;

    private Instant paidAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawPayload;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    @Column(name = "external_trade_no", length = 128)
    private String externalTradeNo;

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
