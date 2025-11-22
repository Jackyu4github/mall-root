package com.mall.domain.user.entity;

import com.mall.domain.register.HumanGender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "sys_user")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SysUser {

    public enum Role {
        SUPER_ADMIN,
        OPERATOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 64)
    private String username;

    @Column(nullable=false)
    private String passwordHash;

    private String realName;


    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "human_gender_enum")
    private HumanGender gender;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;

    @Column(nullable=false)
    private Boolean isActive = true;

    private Instant lastLoginAt;

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
