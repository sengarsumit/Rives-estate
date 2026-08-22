package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID excludeSenderId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID excludeSenderId);
}
