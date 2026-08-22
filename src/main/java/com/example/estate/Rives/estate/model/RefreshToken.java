package com.example.estate.Rives.estate.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

// The raw refresh token value is never stored - only a SHA-256 hash of it
// (see RefreshTokenServiceImpl), so a database read can't be used to forge
// a session. revokedAt null means active; set on rotation (the token was
// exchanged for a new one) or explicit revocation (logout, reuse detected).
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
