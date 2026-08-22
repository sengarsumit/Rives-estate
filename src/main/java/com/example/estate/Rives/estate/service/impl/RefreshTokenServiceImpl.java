package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.model.RefreshToken;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.RefreshTokenRepository;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void issue(User user, String rawToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusMillis(JwtUtil.REFRESH_TOKEN_EXPIRY_MS));
        refreshTokenRepository.save(token);
    }

    @Override
    public User rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.getRevokedAt() != null) {
            // This token was already exchanged for a new one, yet someone is
            // presenting it again - a strong signal it was stolen from an
            // earlier response. Kill every other active session for the user.
            revokeAllActive(stored.getUser());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        return stored.getUser();
    }

    @Override
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private void revokeAllActive(User user) {
        List<RefreshToken> active = refreshTokenRepository.findByUserAndRevokedAtIsNull(user);
        Instant now = Instant.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
