package com.mall.config.minio;

import io.minio.MinioClient;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;

@Configuration
@Profile("prod")
@EnableConfigurationProperties(MinioProps.class)
public class MinioConfigProd {

    @Bean
    public MinioClient minioClient(MinioProps props) throws Exception {
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60));

        // 如果配置了 truststore（用于自签证书），就加载它；否则走系统默认 CA
        if (props.getTls().getTruststorePath() != null && !props.getTls().getTruststorePath().isBlank()) {
            KeyStore ks = KeyStore.getInstance(props.getTls().getType()); // "JKS" or "PKCS12"
            try (FileInputStream in = new FileInputStream(props.getTls().getTruststorePath())) {
                ks.load(in, props.getTls().getTruststorePassword().toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            http.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);
            // 严格校验主机名（默认即可：不要自定义 HostnameVerifier）
        }

        return MinioClient.builder()
                .endpoint(props.getEndpoint())          // 例如 https://minio.mycorp.com:9000
                .credentials(props.getAccessKey(), props.getSecretKey())
                .httpClient(http.build())
                .build();
    }
}

@ConfigurationProperties(prefix = "minio")
@Data
class MinioProps {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private final Tls tls = new Tls();

    @Data
    public static class Tls {
        private String truststorePath;          // 可选：自签证书时使用
        private String truststorePassword;      // 可选
        private String type = "JKS";            // JKS 或 PKCS12
    }
}
