package me.silvernine.tutorial.service;

import me.silvernine.tutorial.config.JwtProperties;
import me.silvernine.tutorial.dto.LoginDto;
import me.silvernine.tutorial.dto.TokenDto;
import me.silvernine.tutorial.entity.Authority;
import me.silvernine.tutorial.entity.RefreshToken;
import me.silvernine.tutorial.entity.User;
import me.silvernine.tutorial.exception.InvalidRefreshTokenException;
import me.silvernine.tutorial.jwt.TokenProvider;
import me.silvernine.tutorial.repository.RefreshTokenRepository;
import me.silvernine.tutorial.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;

/**
 * 로그인, 토큰 재발급, 로그아웃.
 * <p>
 * Access Token 은 짧게(기본 30분) 쓰고, 만료되면 Refresh Token 으로 새로 받습니다.
 * Refresh Token 은 한 번 쓰면 폐기하고 새것을 발급합니다(rotation). 탈취된 토큰을 누가 먼저 쓰면
 * 다른 쪽은 401을 받으므로 유출을 알아챌 수 있습니다.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public TokenDto login(LoginDto loginDto) {
        // 아이디와 비밀번호 확인은 AuthenticationManager 가 CustomUserDetailsService 와 PasswordEncoder 로 합니다.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginDto.username(), loginDto.password()));

        User user = userRepository.findOneWithAuthoritiesByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(authentication.getName()));
        return issueTokens(user, authentication.getAuthorities());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenDto refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("유효하지 않은 Refresh Token입니다."));

        // 쓴 토큰은 결과와 상관없이 바로 폐기합니다.
        refreshTokenRepository.delete(stored);

        if (stored.isExpired(Instant.now())) {
            throw new InvalidRefreshTokenException("만료된 Refresh Token입니다. 다시 로그인하세요.");
        }
        User user = stored.getUser();
        if (!user.isActivated()) {
            throw new InvalidRefreshTokenException("비활성화된 계정입니다.");
        }
        return issueTokens(user, authoritiesOf(user));
    }

    /** 로그아웃: Refresh Token 을 폐기합니다. Access Token 은 만료될 때까지 유효하므로 짧게 둡니다. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private TokenDto issueTokens(User user, Collection<? extends GrantedAuthority> authorities) {
        String accessToken = tokenProvider.createAccessToken(user.getUsername(), authorities);

        String refreshToken = newRefreshToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.save(new RefreshToken(user, hash(refreshToken), expiresAt));

        return new TokenDto(accessToken, refreshToken);
    }

    private static Collection<GrantedAuthority> authoritiesOf(User user) {
        return user.getAuthorities().stream()
                .map(Authority::getAuthorityName)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name))
                .toList();
    }

    /** 추측할 수 없는 256비트 난수. JWT 가 아니라 DB 에서 찾는 열쇠일 뿐입니다. */
    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
