package com.mall.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTool {
    public static void main(String[] args) {
        String rawPassword = "123456";

        // 这是你数据库里拿出来的密文，贴上去检查
        String dbHash = "$2a$10$QeE4N4kQfbt2TKHg4yDnJePfkFlN4P0Q3K4qSCDml4noa0ea52yBa";

        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

        // 1) 生成一个新的hash供你插库
        String newHash = enc.encode(rawPassword);
        System.out.println("新生成的密文: " + newHash);

        // 2) 校验现有密文是不是这个明文
        boolean ok = enc.matches(rawPassword, dbHash);
        System.out.println("数据库这条密文是否等于明文123456: " + ok);
    }
}
