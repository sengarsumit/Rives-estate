package com.example.estate.Rives.estate.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    // Null means unread. Set when the other participant opens the conversation.
    private Instant readAt;
}
