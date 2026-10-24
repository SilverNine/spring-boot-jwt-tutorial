package me.silvernine.tutorial.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import me.silvernine.tutorial.config.JwtProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Access Token(JWT)을 만들고 검증합니다.
 * <p>
 * 강의 영상과 달라진 점
 * <ul>
 *   <li>키는 생성자에서 바로 만듭니다. InitializingBean 이 필요 없습니다.</li>
 *   <li>권한은 쉼표로 이은 문자열 대신 JSON 배열로 넣습니다.</li>
 *   <li>발급 시각(iat)을 넣습니다.</li>
 *   <li>검증과 파싱을 한 번에 합니다. 잘못된 토큰이면 {@link JwtException} 이 납니다.</li>
 * </ul>
 */
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";

    private final SecretKey key;
    private final Duration accessTokenValidity;

    public TokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenValidity = properties.accessTokenValidity();
    }

    public String createAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(username)
                .claim(AUTHORITIES_KEY, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 토큰을 검증하고 인증 정보를 만듭니다.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료된 토큰
     * @throws JwtException                        서명이 틀리거나 형식이 잘못된 토큰
     * @throws IllegalArgumentException            빈 토큰
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<?> roles = claims.get(AUTHORITIES_KEY, List.class);
        List<SimpleGrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
                .map(Object::toString)
                .map(SimpleGrantedAuthority::new)
                .toList();

        User principal = new User(claims.getSubject(), "", authorities);
        return UsernamePasswordAuthenticationToken.authenticated(principal, token, authorities);
    }

    public Duration accessTokenValidity() {
        return accessTokenValidity;
    }
}
