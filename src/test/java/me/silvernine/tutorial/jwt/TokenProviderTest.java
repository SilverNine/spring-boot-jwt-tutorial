package me.silvernine.tutorial.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import me.silvernine.tutorial.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 스프링 없이 TokenProvider 만 검사합니다. 키는 테스트마다 새로 만듭니다. */
class TokenProviderTest {

    private static String randomSecret() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static TokenProvider provider(String secret, Duration validity) {
        return new TokenProvider(new JwtProperties(secret, validity, Duration.ofDays(14)));
    }

    private static final List<GrantedAuthority> ROLES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));

    @Test
    void createdToken_parsesBackToSameUserAndRoles() {
        TokenProvider provider = provider(randomSecret(), Duration.ofMinutes(30));

        Authentication authentication = provider.getAuthentication(provider.createAccessToken("alice", ROLES));

        assertThat(authentication.getName()).isEqualTo("alice");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void expiredToken_throwsExpiredJwtException() {
        TokenProvider provider = provider(randomSecret(), Duration.ofSeconds(-1));
        String token = provider.createAccessToken("alice", ROLES);

        assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String token = provider(randomSecret(), Duration.ofMinutes(30)).createAccessToken("alice", ROLES);
        TokenProvider other = provider(randomSecret(), Duration.ofMinutes(30));

        assertThatThrownBy(() -> other.getAuthentication(token)).isInstanceOf(JwtException.class);
    }
}
