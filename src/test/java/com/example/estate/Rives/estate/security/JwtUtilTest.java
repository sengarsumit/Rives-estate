package com.example.estate.Rives.estate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // HS256 requires a key of at least 256 bits; anything shorter throws
        // WeakKeyException from Keys.hmacShaKeyFor - already-enforced "strong
        // secret" behavior this test incidentally relies on to even construct
        // a JwtUtil, so a too-short key here would fail loudly, not silently.
        ReflectionTestUtils.setField(jwtUtil, "secret_key", "a".repeat(32));
        jwtUtil.init();
    }

    @Test
    void generateRefreshToken_calledTwiceForTheSameUser_neverProducesTheSameToken() {
        // Regression test: without a jti claim, two tokens for the same user
        // issued within the same millisecond (iat/exp identical) were
        // byte-for-byte identical, which silently broke rotation and
        // collided with the refresh_token table's unique hash constraint.
        String first = jwtUtil.generateRefreshToken("alice", "USER");
        String second = jwtUtil.generateRefreshToken("alice", "USER");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generateAccessToken_calledTwiceForTheSameUser_neverProducesTheSameToken() {
        String first = jwtUtil.generateAccessToken("alice", "USER");
        String second = jwtUtil.generateAccessToken("alice", "USER");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generatedToken_roundTripsUsernameAndRole() {
        String token = jwtUtil.generateAccessToken("alice", "DEALER");

        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("DEALER");
        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
    }

    @Test
    void validateJwtToken_malformedToken_returnsFalse() {
        assertThat(jwtUtil.validateJwtToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateJwtToken_tokenSignedWithADifferentKey_returnsFalse() {
        JwtUtil otherIssuer = new JwtUtil();
        ReflectionTestUtils.setField(otherIssuer, "secret_key", "b".repeat(32));
        otherIssuer.init();
        String tokenFromOtherIssuer = otherIssuer.generateAccessToken("alice", "USER");

        assertThat(jwtUtil.validateJwtToken(tokenFromOtherIssuer)).isFalse();
    }
}
