package com.mall.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    public enum SaleMode {
        SALE,
        RENT_ONLY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=256)
    private String productNumber;

    private Long categoryId;

    @Column(nullable=false, length=256)
    private String productName;

    @Column(length=512)
    private String productSubtitle;

    @Column(columnDefinition = "TEXT")
    private String descriptionText;

    @Column(columnDefinition = "TEXT")
    private String wlText;

    @Column(columnDefinition = "TEXT")
    private String mainImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private SaleMode saleMode = SaleMode.SALE;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceSale;

    @Column(nullable=false, length=128)
    private String priceUnit;

    private Integer sortProduct;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceRentPerDay;
    @Column(precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable=false)
    private Boolean isOnShelf = true;

    @Column(nullable=false)
    private Boolean isWanlian = true;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    // 可保留，但一般不用于更新
    /** 只读：从 inventory 表“派生”出库存数量（无行则为 null） */
    @Formula("(select i.stock_qty from inventory i where i.product_id = id)")
    private Integer stockQty;

    // 同上
    /** 只读：从 inventory 表“派生”出可租数量（无行则为 null） */
    @Formula("(select i.available_qty from inventory i where i.product_id = id)")
    private Integer availableQty;

    // 容错 getter，避免前端拿到 null
    public Integer getStockQty() { return stockQty == null ? 0 : stockQty; }

    public Integer getAvailableQty() { return availableQty == null ? 0 : availableQty; }

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
