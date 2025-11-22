package com.mall.common.security;

import com.mall.auth.LoginUserType;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AuthContext {
    private static final ThreadLocal<UserInfo> LOCAL = new ThreadLocal<>();

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private LoginUserType type;
        private String role;
    }

    public static void set(UserInfo info){
        LOCAL.set(info);
    }

    public static UserInfo get(){
        return LOCAL.get();
    }

    public static void clear(){
        LOCAL.remove();
    }
}
