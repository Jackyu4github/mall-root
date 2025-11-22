package com.mall.domain.register;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "register",
    indexes = {
        @Index(name = "idx_register_time", columnList = "register_time"),
        @Index(name = "idx_address_id", columnList = "address_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_register_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_register_identity_id", columnNames = "identity_id"),
        @UniqueConstraint(name = "uk_register_related_name", columnNames = "related_name"),
        @UniqueConstraint(name = "uk_register_related_mobile", columnNames = "related_mobile")
    }
)
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "register_time", nullable = false)
    private Instant registerTime;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "identity_id", nullable = false, length = 256)
    private String identityId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "gender", columnDefinition = "human_gender_enum")
    private HumanGender gender;

    @Column(name = "related_name", nullable = false, length = 256)
    private String relatedName;

    @Column(name = "related_mobile", nullable = false, length = 256)
    private String relatedMobile;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "address_name", length = 256)
    private String addressName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "address_status", nullable = false, columnDefinition = "register_address_enum")
    private RegisterAddressStatus addressStatus;   // YES / NO

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (registerTime == null) registerTime = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (active == null) active = Boolean.TRUE;
        if (addressStatus == null) addressStatus = RegisterAddressStatus.NO; // 与表默认一致
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
