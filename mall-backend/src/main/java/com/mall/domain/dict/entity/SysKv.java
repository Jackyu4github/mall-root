package com.mall.domain.dict.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "sys_kv")
public class SysKv {

    @Id
    @Column(name="k", nullable=false, length=128)
    private String k;

    @Column(name="v", nullable=false, length=512)
    private String v;

    @Column(name="updated_at", nullable=false, columnDefinition = "timestamptz")
    private Instant updatedAt;

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
