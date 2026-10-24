package me.silvernine.tutorial.service;

import me.silvernine.tutorial.entity.Authority;
import me.silvernine.tutorial.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인할 때 DB 의 사용자를 Spring Security 의 UserDetails 로 바꿉니다.
 * <p>
 * 비활성 계정은 {@code disabled(true)} 로 표시만 하면 Spring Security 가 DisabledException 을 냅니다.
 * 여기서 쓰는 User 는 Spring Security 의 User 이고, entity.User 와 다른 클래스입니다.
 */
@Service("userDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findOneWithAuthoritiesByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities(user.getAuthorities().stream()
                                .map(Authority::getAuthorityName)
                                .toArray(String[]::new))
                        .disabled(!user.isActivated())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username + " -> 데이터베이스에서 찾을 수 없습니다."));
    }
}
