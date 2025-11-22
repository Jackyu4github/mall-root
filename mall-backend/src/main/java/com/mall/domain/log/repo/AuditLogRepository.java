package com.mall.domain.log.repo;

import com.mall.domain.log.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
