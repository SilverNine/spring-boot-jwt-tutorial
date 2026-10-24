package me.silvernine.tutorial.repository;

import me.silvernine.tutorial.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"user", "user.authorities"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
