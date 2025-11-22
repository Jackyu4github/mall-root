package com.mall.domain.address.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "address_code",
       indexes = {
           @Index(name = "idx_qr_category", columnList = "category_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_address_code_code", columnNames = "code"),
           @UniqueConstraint(name = "uk_address_code_name", columnNames = "name"),
           @UniqueConstraint(name = "uk_address_code_category", columnNames = "category"),
           @UniqueConstraint(name = "uk_address_code_qr", columnNames = "qr_code_value")
       })
public class AddressCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 编号
    @Column(nullable = false, length = 256)
    private String code;

    // 名称
    @Column(nullable = false, length = 256)
    private String name;

    // 类别（注意：DDL 要求 UNIQUE）
    @Column(nullable = false, length = 256)
    private String category;

    // 关联 product_category(id)
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    // 图片
    @Column(name = "img_url", length = 256)
    private String imgUrl;

    // 关联页面
    @Column(name = "related_pages", length = 64)
    private Long relatedPages;

    // 扫码解析出来的短码/码值（唯一）
    @Column(name = "qr_code_value", nullable = false, length = 256)
    private String qrCodeValue;

    // 是否启用
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    // 创建人（sys_user.id）
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (active == null) active = Boolean.TRUE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
