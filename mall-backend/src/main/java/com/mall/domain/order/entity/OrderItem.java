package com.mall.domain.order.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mall.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="order_item")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

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

    private Long orderId;

    private Long productId;

    @Column(nullable=false, length=256)
    private String productNameSnap;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private Product.SaleMode saleModeSnap;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private Order.Status status = Order.Status.CREATED;

    @Column(nullable=false, precision=18, scale=2)
    private BigDecimal unitPrice;

    @Column(nullable=false)
    private Integer quantity;

    private Integer rentDays;

    /**
     * ext_field JSONB：用 JsonNode 存，Hibernate 负责 JSON ↔ SQL 映射
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ext_field", columnDefinition = "jsonb")
    private JsonNode extField;


    @Column(nullable=false, precision=18, scale=2)
    private BigDecimal lineAmount;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
    }

}
