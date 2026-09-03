package com.scorenow.scorenow_api.global.config;

import com.scorenow.scorenow_api.domain.user.jwt.JwtAuthenticationFilter;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;

import com.scorenow.scorenow_api.global.config.properties.CorsProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ← 추가
                .csrf(csrf -> csrf.disable())
                //jwt사용 세션 stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/images").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/posts").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/community/posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/community/posts/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/community/comments/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/community/posts/*/reaction").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/reports").authenticated()
                        // Swagger 관련 리소스 전체 접근 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/test/betsapi/**"
                        ).permitAll()
                        // 토큰없이 접근 가능한 인증 API
                        .requestMatchers(
                                "/api/v1/auth/social-login",
                                "/api/v1/auth/refresh",
                                "/ws/**",
                                "/ws"
                        ).permitAll()
                        .requestMatchers("/api/v1/app/**").permitAll()    // 앱 전용 API 접근 허용
                        .requestMatchers("/api/v1/admin/**").permitAll()  // Admin API 접근 허용 (임시:authenticated 로 변경 필요)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키/인증 헤더 허용
        config.setMaxAge(3600L); // preflight 캐시 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
