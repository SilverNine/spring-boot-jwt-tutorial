package me.silvernine.tutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 설정. SecurityConfig 의 {@code http.cors(withDefaults())} 가 이 UrlBasedCorsConfigurationSource 빈을 찾아 씁니다
 * (Spring Security 공식 문서, CORS 절).
 * 강의 영상처럼 CorsFilter 를 직접 필터 체인에 끼울 필요가 없습니다.
 */
@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Authorization 헤더로 토큰을 보내는 방식이라 쿠키 자격증명이 필요 없습니다.
        // allowCredentials=false 이므로 모든 오리진을 허용해도 안전합니다.
        // 운영에서는 프런트엔드 주소만 허용하세요. 예: List.of("https://app.example.com")
        config.setAllowCredentials(false);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
