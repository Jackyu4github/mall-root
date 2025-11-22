package com.mall.auth;

public record AuthUser(Long userId, LoginUserType type, String role) {}
