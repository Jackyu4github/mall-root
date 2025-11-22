package com.mall.common.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.UUID;

public final class IdGenerator {
    private IdGenerator(){}

    private static String ts(){
        // yyyyMMddHHmmss + epochMilliTail
        long ms = System.currentTimeMillis();
        String iso = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(ms));
        return iso + (ms % 1000);
    }

    public static String newOrderNo(){
        return "O" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public static String newPaymentNo(){
        return "P" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public static String newRefundNo(){
        return "R" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public static String newProductNo(){
        return "PD" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public static String newProductCategoryNo(){
        return "PDC" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public static String newAddressNo(){
        return "A" + ts() + "_" + UUID.randomUUID().toString().substring(0,8);
    }
}
