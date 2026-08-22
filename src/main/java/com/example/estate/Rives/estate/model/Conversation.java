package com.example.estate.Rives.estate.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "buyer_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Property property;

    // The non-dealer participant. The dealer side of the conversation is
    // always derived from property.getDealer(), never stored redundantly.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User buyer;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    // Denormalized so the inbox list can sort/display without a join or
    // subquery over messages; kept in sync by ChatServiceImpl on every send.
    private Instant lastMessageAt;

    @Column(length = 160)
    private String lastMessagePreview;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Message> messages = new ArrayList<>();
}
