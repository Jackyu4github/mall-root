package com.mall.common.security;

import com.mall.auth.AuthUser;
import com.mall.auth.JwtCodec;
import com.mall.auth.LoginUserType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${app.security.jwt-secret}")
    private String secret;

    @Value("${app.security.jwt-issuer:mall-backend}")
    private String issuer;

    private static final long ACCESS_EXPIRE_SECONDS  = 8 * 60 * 60;
    private static final long REFRESH_EXPIRE_SECONDS = 30L * 24 * 60 * 60;

    private JwtCodec codec;

    @PostConstruct
    public void init() {
        this.codec = new JwtCodec(secret, issuer, ACCESS_EXPIRE_SECONDS, REFRESH_EXPIRE_SECONDS);
    }

    public String signAccessToken(Long uid, LoginUserType userType, String role) {
        return codec.signAccessToken(uid, userType, role);
    }

    public String signRefreshToken(Long uid, LoginUserType userType, String role) {
        return codec.signRefreshToken(uid, userType, role);
    }

    public String generateTokenForAppUser(Long appUserId) {
        return signAccessToken(appUserId, LoginUserType.APP_USER, "APP_USER");
    }

    public String generateRefreshTokenForAppUser(Long appUserId) {
        return signRefreshToken(appUserId, LoginUserType.APP_USER, "APP_USER");
    }

    public Tokens generateTokenPairForAppUser(Long appUserId) {
        String access  = generateTokenForAppUser(appUserId);
        String refresh = generateRefreshTokenForAppUser(appUserId);
        long expSec  = accessExpireEpochSeconds();
        return new Tokens(access, refresh, expSec);
    }

    public static final class Tokens {
        public final String accessToken;
        public final String refreshToken;
        public final long   accessExpireAt;
        public Tokens(String a, String r, long e) {
            this.accessToken = a; this.refreshToken = r; this.accessExpireAt = e;
        }
    }

    // ✅ 继续保持你 backend 的返回类型不变
    public AuthContext.UserInfo parseToken(String jwt) {
        AuthUser u = codec.parseAccessAuthUser(jwt);
        if (u == null) return null;
        return new AuthContext.UserInfo(u.userId(), u.type(), u.role());
    }

    public AuthContext.UserInfo parseRefresh(String refreshJwt) {
        AuthUser u = codec.parseRefreshAuthUser(refreshJwt);
        if (u == null) return null;
        return new AuthContext.UserInfo(u.userId(), u.type(), u.role());
    }

    public long accessExpireEpochSeconds() {
        return codec.accessExpireEpochSeconds();
    }

    // ✅ 如果你想让 backend 也用 AuthUser（可选）
    public AuthUser parseAccessAuthUser(String jwt) {
        return codec.parseAccessAuthUser(jwt);
    }
}
