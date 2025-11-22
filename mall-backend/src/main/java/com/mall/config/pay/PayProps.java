// com.mall.config.pay.PayProps
package com.mall.config.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app.pay")
public class PayProps {
    private String notifyDomain;
    private String returnUrl;

    private Wx wx = new Wx();
    private Ali ali = new Ali();

    @Data
    public static class Wx {
        private String mchId;
        private String appId;
        private String appIdNative;
        private String apiV3Key;
        private String serialNo;
        private String privateKeyPath;
        private String platformCertPath;
        private String notifyPath;
    }

    @Data
    public static class Ali {
        private String appId;
        private String privateKey;
        private String alipayPublicKey;
        private String gateway;
        private String charset;
        private String signType;
        private String format;
        private String notifyPath;
    }
}
