package com.mall.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    private Long productId;

    @Column(name = "product_name")
    private String productName;           // 冗余的商品名（业务代码维护）

    @Column(nullable=false)
    private Integer stockQty = 0;

    @Column(nullable=false)
    private Integer availableQty = 0;

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "v", nullable = false)
    private Long version = 0L;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

}
