package com.mall.domain.pay;

public enum TradeType {
    WECHAT_NATIVE,   // PC 扫码
    WECHAT_JSAPI,    // 公众号/小程序/微信内H5
    ALIPAY_PC,       // 电脑网站
    ALIPAY_WAP       // 手机网站
}