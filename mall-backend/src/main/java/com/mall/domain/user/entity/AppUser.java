package com.mall.domain.user.entity;

import com.mall.domain.register.HumanGender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true, length=32)
    private String phone;

    @Column(unique=true, length=128)
    private String email;

    @Column(nullable=false)
    private String passwordHash;

    private String wxOpenid;
    private String nickname;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "human_gender_enum")
    private HumanGender gender;

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
