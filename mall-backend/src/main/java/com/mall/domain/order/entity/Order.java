package com.mall.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="\"order\"") // order是保留字，加引号
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    public enum Status {
        CREATED,
        PAID,
        RECEIVED,
        SHIPPED,
        COMPLETED,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable=false, unique=true, length=64)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private Status status = Status.CREATED;

    @Column(nullable=false, precision=18, scale=2)
    private BigDecimal totalAmount;

    @Column(nullable=false, precision=18, scale=2)
    private BigDecimal payableAmount;

    @Column(nullable=false, length=8)
    private String currencyCode = "CNY";

    @Column(length=64)
    private String contactName;
    @Column(length=32)
    private String contactPhone;

    private Long registerId;
    @Column(length=64)
    private String registerName;

    private Long addressId;

    @Column(columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(nullable=false)
    private Boolean needWl = true;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    private Instant payAt;

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
