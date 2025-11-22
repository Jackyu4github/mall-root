package com.mall.domain.user.repo;

import com.mall.domain.user.entity.AppUser;
import com.mall.domain.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);

    // 后台列表页按注册时间倒序
    List<SysUser> findAllByOrderByCreatedAtDesc();

    // 关键字搜索（手机号 / 邮箱 / 昵称 模糊匹配）
    @Query("""
        SELECT u
        FROM SysUser u
        WHERE
            (:kw IS NULL OR :kw = '' OR
             LOWER(u.username)    LIKE LOWER(CONCAT('%', :kw, '%')) OR
             LOWER(u.realName)    LIKE LOWER(CONCAT('%', :kw, '%'))
            )
        ORDER BY u.createdAt DESC
        """)
    List<SysUser> search(@Param("kw") String keyword);
}
