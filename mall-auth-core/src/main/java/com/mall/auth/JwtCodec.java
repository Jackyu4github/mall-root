package com.mall.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class JwtCodec {

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtCodec(String secret, String issuer,
                    long accessExpireSeconds,
                    long refreshExpireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessExpireSeconds = accessExpireSeconds;
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    // === 生成 AccessToken（保持 backend 语义） ===
    public String signAccessToken(Long uid, LoginUserType userType, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("uid",  uid)
                .claim("type", userType)
                .claim("role", role)
                .claim("tok",  "ACCESS")
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpireSeconds)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // === 生成 RefreshToken ===
    public String signRefreshToken(Long uid, LoginUserType userType, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("uid",  uid)
                .claim("type", userType)
                .claim("role", role)
                .claim("tok",  "REFRESH")
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshExpireSeconds)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // === notice-service 用：只接受 ACCESS，返回 AuthUser ===
    public AuthUser parseAccessAuthUser(String jwt) {
        Claims c = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        if (!"ACCESS".equals(c.get("tok", String.class))) return null;
        return toAuthUser(c);
    }

    // === backend 刷新接口用：只接受 REFRESH，返回 AuthUser ===
    public AuthUser parseRefreshAuthUser(String jwt) {
        Claims c = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        if (!"REFRESH".equals(c.get("tok", String.class))) return null;
        return toAuthUser(c);
    }

    public long accessExpireEpochSeconds() {
        return Instant.now().plusSeconds(accessExpireSeconds).getEpochSecond();
    }

    private AuthUser toAuthUser(Claims c) {
        Long uid = ((Number) c.get("uid")).longValue();

        LoginUserType userType;
        Object typeClaim = c.get("type");
        if (typeClaim instanceof LoginUserType lut) {
            userType = lut;
        } else {
            userType = LoginUserType.valueOf(String.valueOf(typeClaim));
        }

        String role = c.get("role", String.class);
        return new AuthUser(uid, userType, role);
    }
}
