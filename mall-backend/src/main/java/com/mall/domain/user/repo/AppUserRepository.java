package com.mall.domain.user.repo;

import com.mall.domain.user.entity.AppUser;
import com.mall.dto.user.UserRegisterRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByWxOpenid(String wxOpenid);


    Optional<AppUser> findByPhone(String phone);
    Optional<AppUser> findByEmail(String email);

    // 后台列表页按注册时间倒序
    List<AppUser> findAllByOrderByCreatedAtDesc();

    // 关键字搜索（手机号 / 邮箱 / 昵称 模糊匹配）
    @Query("""
        SELECT u
        FROM AppUser u
        WHERE
            (:kw IS NULL OR :kw = '' OR
             LOWER(u.phone)    LIKE LOWER(CONCAT('%', :kw, '%')) OR
             LOWER(u.email)    LIKE LOWER(CONCAT('%', :kw, '%')) OR
             LOWER(u.nickname) LIKE LOWER(CONCAT('%', :kw, '%'))
            )
        ORDER BY u.createdAt DESC
        """)
    List<AppUser> search(@Param("kw") String keyword);

    @Query(
            value = """
        SELECT
            u.id,
            u.nickname,
            u.phone,
            u.email,
            u.wx_openid,
            u.created_at,
            u.last_login_at,
            COALESCE(
                string_agg(DISTINCT o.register_name, ','), 
                ''
            ) AS "registerNames"
        FROM app_user u
        LEFT JOIN "order" o ON o.user_id = u.id
        WHERE (:userId IS NULL OR u.id = :userId)
        GROUP BY u.id, u.nickname, u.phone, u.email, u.wx_openid, u.created_at, u.last_login_at
        ORDER BY u.id
        """,
            countQuery = """
        SELECT COUNT(1)
        FROM app_user u
        WHERE (:userId IS NULL OR u.id = :userId)
        """,
            nativeQuery = true
    )
    Page<UserRegisterRow> pageUsersWithRegisterNameStr(@Param("userId") Long userId, Pageable pageable);



    @Query(
            value = """
            SELECT
                u.id,
                u.nickname,
                u.phone,
                u.email,
                u.wx_openid,
                u.created_at,
                u.last_login_at,
                COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT o.register_name), NULL), '{}') AS "registerNames"
            FROM app_user u
            LEFT JOIN "order" o ON o.user_id = u.id
            WHERE (:userId IS NULL OR u.id = :userId)
            GROUP BY u.id, u.nickname, u.phone, u.email
            ORDER BY u.id
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM app_user u
            WHERE (:userId IS NULL OR u.id = :userId)
            """,
            nativeQuery = true
    )
    Page<UserRegisterRow> pageUsersWithRegisterNameList(@Param("userId") Long userId, Pageable pageable);
}
