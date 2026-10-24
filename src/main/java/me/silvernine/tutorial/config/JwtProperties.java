package me.silvernine.tutorial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml 의 jwt.* 설정을 타입이 있는 객체로 받습니다.
 * 강의 영상의 {@code @Value("${jwt.secret}")} 방식 대신 쓰는 요즘 방식입니다.
 *
 * @param secret               Base64로 인코딩한 HS512 서명 키 (64바이트 이상)
 * @param accessTokenValidity  Access Token 유효 시간 (예: 30m)
 * @param refreshTokenValidity Refresh Token 유효 시간 (예: 14d)
 */
@ConfigurationProperties("jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenValidity,
        Duration refreshTokenValidity
) {
}
