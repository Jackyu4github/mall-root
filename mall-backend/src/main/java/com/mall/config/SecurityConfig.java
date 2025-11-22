package com.mall.config;

import com.mall.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 前后端分离API，不要CSRF/表单登录/Basic弹窗
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // 统一用JWT，不用session记状态
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 统一的未登录/无权限返回，转成JSON
            .exceptionHandling(ex -> ex
                // 没登录（没有或无效JWT）访问受保护接口时 -> 401
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"msg\":\"UNAUTHORIZED\"}");
                })
                // 登录了但权限不够时 -> 403
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"msg\":\"FORBIDDEN\"}");
                })
            )

            // 路由访问控制
            .authorizeHttpRequests(auth -> auth

                // ===== Swagger / 文档，放行 =====
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()

                // ===== 健康检查等公共接口，放行 =====
                .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/metrics/**",      // ⬅⬅ 加上
                        "/actuator/prometheus"       // ⬅⬅ 可选，加上更方便抓指标
                ).permitAll()

                // ===== 静态资源或公开资源（按自己项目需要补）=====
                .requestMatchers(HttpMethod.GET,
                        "/public/**",
                        "/files/**"
                ).permitAll()

                // ===== 登录 / 注册 / 刷新token 必须允许未登录访问 =====
                // 你现在实际打的是 /admin/v1/auth/login
                .requestMatchers(
                        "/admin/v1/auth/**",
                        "api/v1/auth/**"
                ).permitAll()

                .requestMatchers(
                        "/api/v1/pay/**"
                ).permitAll()

                // ⭐⭐ WebSocket握手先放行，否则根本升级不了 ⭐⭐
                .requestMatchers(
                        "/ws/**"
                ).permitAll()

                // ===== 其他所有接口必须带上有效JWT =====
                .anyRequest().authenticated()
            )

            // 把我们自己的 JWT 过滤器放在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ✅ 启用 CORS，使用下方自定义的 CorsConfigurationSource
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    // 登录代码里如果要用 AuthenticationManager 来校验用户名密码，这里暴露一个Bean出来
    // ✅ 暴露 AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ✅ 跨域配置：支持所有来源（开发期），全方法/头，暴露 Authorization 等头
    // 生产建议把 allowedOriginPatterns 改为你的域名白名单
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 如果需要支持带 Cookie 的跨域，把 allowCredentials(true) 同时把下面的 * 改成明确域名
        cfg.setAllowCredentials(false);
        cfg.setAllowedOriginPatterns(List.of("*")); // 生产改成 List.of("https://a.example.com","https://b.example.com")
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization","Content-Disposition","X-Request-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
