package com.example.estate.Rives.estate.service;

import com.example.estate.Rives.estate.model.User;

public interface RefreshTokenService {
    void issue(User user, String rawToken);

    // Validates the presented refresh token and rotates it: the token is
    // marked used (revoked) and the caller is expected to issue + issue() a
    // brand new one. Throws ApiException(401) if the token is unknown,
    // expired, or already used - reuse of an already-rotated token revokes
    // every other active token for that user (theft response).
    User rotate(String rawToken);

    void revoke(String rawToken);
}
