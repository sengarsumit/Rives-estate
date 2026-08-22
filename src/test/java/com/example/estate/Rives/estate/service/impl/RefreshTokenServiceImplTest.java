package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.model.RefreshToken;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(refreshTokenRepository);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setRole(Role.USER);
    }

    private RefreshToken storedFor(String rawToken, User owner) {
        RefreshToken stored = new RefreshToken();
        stored.setId(UUID.randomUUID());
        stored.setUser(owner);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        return stored;
    }

    @Test
    void issue_savesAHashOfTheToken_neverTheRawToken() {
        service.issue(user, "raw-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotEqualTo("raw-refresh-token");
        assertThat(saved.getTokenHash()).hasSize(64); // hex-encoded SHA-256
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void issue_isDeterministic_soTheSameRawTokenAlwaysHashesTheSameWay() {
        service.issue(user, "raw-refresh-token");
        service.issue(user, "raw-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        List<RefreshToken> saved = captor.getAllValues();
        assertThat(saved.get(0).getTokenHash()).isEqualTo(saved.get(1).getTokenHash());
    }

    @Test
    void rotate_activeToken_marksItRevokedAndReturnsTheOwner() {
        RefreshToken stored = storedFor("raw", user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        User result = service.rotate("raw");

        assertThat(result).isEqualTo(user);
        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void rotate_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void rotate_expiredToken_throwsUnauthorized() {
        RefreshToken stored = storedFor("raw", user);
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rotate_alreadyRevokedToken_revokesEveryActiveTokenForThatUserAndThrows() {
        RefreshToken reused = storedFor("raw", user);
        reused.setRevokedAt(Instant.now().minusSeconds(30));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(reused));

        RefreshToken otherActive = storedFor("other-raw", user);
        when(refreshTokenRepository.findByUserAndRevokedAtIsNull(user)).thenReturn(List.of(otherActive));

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid refresh token");

        assertThat(otherActive.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).saveAll(List.of(otherActive));
    }

    @Test
    void revoke_activeToken_marksItRevoked() {
        RefreshToken stored = storedFor("raw", user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        service.revoke("raw");

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revoke_unknownToken_isANoOp() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revoke("raw");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revoke_alreadyRevokedToken_doesNotSaveAgain() {
        RefreshToken stored = storedFor("raw", user);
        stored.setRevokedAt(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        service.revoke("raw");

        verify(refreshTokenRepository, never()).save(any());
    }
}
