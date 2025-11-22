package com.mall.domain.log.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="audit_log")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "SYS_USER"/"APP_USER"
    @Column(length=32)
    private String actorType;

    private Long actorId;

    @Column(length=64)
    private String action;

    @Column(length=64)
    private String targetType;

    private Long targetId;

    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
    }

}
