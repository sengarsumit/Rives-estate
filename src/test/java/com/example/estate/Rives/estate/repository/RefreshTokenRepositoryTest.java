package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.config.JpaAuditingConfig;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.RefreshToken;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password123");
        user.setRole(Role.USER);
        return entityManager.persistAndFlush(user);
    }

    private RefreshToken persistToken(User user, String hash, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        return entityManager.persistAndFlush(token);
    }

    @Test
    void findByTokenHash_returnsToken_whenItExists() {
        User user = persistUser("alice");
        RefreshToken token = persistToken(user, "hash-1", Instant.now().plusSeconds(3600), null);

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash("hash-1");

        assertThat(result).contains(token);
    }

    @Test
    void findByTokenHash_returnsEmpty_whenHashIsUnknown() {
        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash("does-not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserAndRevokedAtIsNull_returnsOnlyThatUsersActiveTokens() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        RefreshToken aliceActive = persistToken(alice, "alice-active", Instant.now().plusSeconds(3600), null);
        persistToken(alice, "alice-revoked", Instant.now().plusSeconds(3600), Instant.now());
        persistToken(bob, "bob-active", Instant.now().plusSeconds(3600), null);

        List<RefreshToken> results = refreshTokenRepository.findByUserAndRevokedAtIsNull(alice);

        assertThat(results).containsExactly(aliceActive);
    }

    @Test
    void save_rejectsDuplicateTokenHash() {
        User user = persistUser("alice");
        persistToken(user, "duplicate-hash", Instant.now().plusSeconds(3600), null);

        RefreshToken duplicate = new RefreshToken();
        duplicate.setUser(user);
        duplicate.setTokenHash("duplicate-hash");
        duplicate.setExpiresAt(Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
