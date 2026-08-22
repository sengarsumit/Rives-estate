package com.example.estate.Rives.estate.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L;
    public static final long REFRESH_TOKEN_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    @Value("${jwt.secret.key}")
    private String secret_key;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret_key.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT Secret key initialized.");
    }

    // short-period access tokens
    public String generateAccessToken(String username,String role) {
        logger.info("Generating access token for user: {},role{}", username,role);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    //long-period refresh tokens
    public String generateRefreshToken(String username,String role) {
        logger.info("Generating refresh token for user: {},role{}", username,role);
        // A random jti (rather than relying on iat/exp alone) guarantees two
        // tokens for the same user are never byte-identical even when issued
        // within the same millisecond (e.g. signin immediately followed by a
        // refresh) - without it, "rotation" could silently produce the same
        // token again, and the DB's unique constraint on the token hash would
        // reject the second insert.
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        String username = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        logger.info("Extracted username from token: {}", username);
        return username;
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            logger.info("JWT token is valid.");
            return true;
        } catch (SignatureException e) {
            // NOT java.lang.SecurityException (an unrelated JDK class this
            // used to catch by mistake, which meant a tampered/wrong-signature
            // token fell through every catch here and propagated uncaught -
            // harmless everywhere this was called from behind another
            // catch-all, until AuthController's refresh endpoint started
            // calling it directly and turned that into an unhandled 500.
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            // Catch-all for any other JJWT parsing/validation failure not
            // enumerated above, so this method can never throw for a token
            // that's simply invalid - only for a genuinely unexpected error.
            logger.error("JWT token could not be validated: {}", e.getMessage());
        }
        return false;
    }
}
