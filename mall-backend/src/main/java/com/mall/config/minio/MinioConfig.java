package com.mall.config.minio;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
@Profile("dev") // 只在 dev 配置生效：spring.profiles.active=dev
public class MinioConfig {

    @Bean
    public MinioClient minioClient() throws Exception {
        // 1) 信任所有证书（仅用于开发）
        X509TrustManager trustAll = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS"); // 支持到 TLSv1.2/1.3
        sslContext.init(null, new TrustManager[]{trustAll}, new SecureRandom());

        // 2) OkHttp 客户端（超时 + 放宽主机名）
        OkHttpClient http = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustAll)
                .hostnameVerifier((hostname, session) -> true) // 仅开发使用
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();

        // 3) MinIO Client
        return MinioClient.builder()
                .endpoint("https://192.168.3.7:9009")  // ⬅ 改成你当前可达的地址/端口/协议
                .credentials("minioadmin", "Daandu@123")  // ⬅ 你的测试凭证
                .httpClient(http)
                .build();
    }
}