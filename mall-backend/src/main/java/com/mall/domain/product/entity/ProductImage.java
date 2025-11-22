package com.mall.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "product_image")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    @Column(columnDefinition = "TEXT", nullable=false)
    private String imageUrl;

    @Column(nullable=false)
    private Integer sortOrder = 0;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
    }

}
