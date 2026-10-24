package me.silvernine.tutorial.dto;

/**
 * 로그인과 재발급 응답.
 *
 * @param token        Access Token(JWT). API 를 부를 때 Authorization: Bearer 헤더에 넣습니다.
 *                     강의 영상과 같은 필드 이름을 유지합니다.
 * @param refreshToken Access Token 이 만료되면 /api/token/refresh 로 보내 새 토큰을 받습니다.
 */
public record TokenDto(String token, String refreshToken) {
}
