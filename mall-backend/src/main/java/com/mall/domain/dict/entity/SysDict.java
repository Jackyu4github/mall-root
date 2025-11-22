package com.mall.domain.dict.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "sys_dict",
       indexes = {
           @Index(name = "idx_sys_dict_type_status_sort", columnList = "dict_type, status, sort"),
           @Index(name = "idx_sys_dict_type_key", columnList = "dict_type, dict_key", unique = true)
       })
public class SysDict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name="dict_type", nullable=false, length=64)
    private String dictType;

    @Column(name="dict_key", nullable=false, length=64)
    private String dictKey;

    @Column(name="dict_value", nullable=false, length=256)
    private String dictValue;

    @Column(name="label", nullable=false, length=128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "dict_status_enum", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DictStatus status; // "ENABLED"/"DISABLED"（保持和你表的枚举一致）

    private String remark;

    @Column(name="sort", nullable=false)
    private Integer sort;

    @Column(name="created_at", nullable=false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name="updated_at", nullable=false, columnDefinition = "timestamptz")
    private Instant updatedAt;

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
