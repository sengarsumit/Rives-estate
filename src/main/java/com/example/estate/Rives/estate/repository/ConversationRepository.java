package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByPropertyIdAndBuyerId(UUID propertyId, UUID buyerId);

    List<Conversation> findByBuyerIdOrProperty_DealerIdOrderByLastMessageAtDesc(UUID buyerId, UUID dealerId);
}
