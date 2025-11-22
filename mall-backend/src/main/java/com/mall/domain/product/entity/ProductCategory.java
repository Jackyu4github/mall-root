package com.mall.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "product_category")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    @Column(nullable=false, length=128)
    private String categoryNumber;

    @Column(nullable=false, length=128)
    private String categoryName;

    @Column(nullable=false, length=128)
    private String categoryIcon;

    @Column(columnDefinition = "TEXT")
    private String categoryDesc;

    @Column(nullable=false, length=128)
    private String quantityUnit;

    @Column(nullable=false)
    private Boolean display = true;

    @Column(nullable=false)
    private Integer sortOrder = 0;

    @Column(nullable=false)
    private Boolean isActive = true;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

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
