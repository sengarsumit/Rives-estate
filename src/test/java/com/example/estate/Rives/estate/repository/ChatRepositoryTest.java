package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.config.JpaAuditingConfig;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.Message;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class ChatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User persistUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password123");
        user.setRole(role);
        return entityManager.persistAndFlush(user);
    }

    private Property persistProperty(String title, User dealer) {
        Property property = new Property();
        property.setTitle(title);
        property.setAddress("123 Main St");
        property.setLocality("HSR");
        property.setRental(15000.0);
        property.setDealer(dealer);
        return entityManager.persistAndFlush(property);
    }

    private Conversation persistConversation(Property property, User buyer, Instant lastMessageAt) {
        Conversation conversation = new Conversation();
        conversation.setProperty(property);
        conversation.setBuyer(buyer);
        conversation.setLastMessageAt(lastMessageAt);
        return entityManager.persistAndFlush(conversation);
    }

    private Message persistMessage(Conversation conversation, User sender, String content, Instant readAt) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setReadAt(readAt);
        return entityManager.persistAndFlush(message);
    }

    @Test
    void findByPropertyIdAndBuyerId_returnsConversation_whenExists() {
        User dealer = persistUser("dealer1", Role.DEALER);
        User buyer = persistUser("buyer1", Role.USER);
        Property property = persistProperty("Villa", dealer);
        Conversation conversation = persistConversation(property, buyer, Instant.now());

        Optional<Conversation> result = conversationRepository.findByPropertyIdAndBuyerId(property.getId(), buyer.getId());

        assertThat(result).contains(conversation);
    }

    @Test
    void findByPropertyIdAndBuyerId_returnsEmpty_whenNoConversation() {
        User dealer = persistUser("dealer2", Role.DEALER);
        User buyer = persistUser("buyer2", Role.USER);
        Property property = persistProperty("Villa", dealer);

        Optional<Conversation> result = conversationRepository.findByPropertyIdAndBuyerId(property.getId(), buyer.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void save_rejectsSecondConversationForSamePropertyAndBuyer() {
        User dealer = persistUser("dealer3", Role.DEALER);
        User buyer = persistUser("buyer3", Role.USER);
        Property property = persistProperty("Villa", dealer);
        persistConversation(property, buyer, Instant.now());

        Conversation duplicate = new Conversation();
        duplicate.setProperty(property);
        duplicate.setBuyer(buyer);
        duplicate.setLastMessageAt(Instant.now());

        assertThatThrownBy(() -> conversationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByBuyerIdOrPropertyDealerId_returnsConversationsForBothRoles_newestActivityFirst() {
        User dealer = persistUser("dealer4", Role.DEALER);
        User buyer = persistUser("buyer4", Role.USER);
        User otherDealer = persistUser("dealer5", Role.DEALER);
        Property ownProperty = persistProperty("Villa as dealer", dealer);
        Property otherProperty = persistProperty("Villa as buyer", otherDealer);

        Instant now = Instant.now();
        Conversation asDealer = persistConversation(ownProperty, buyer, now.minus(1, ChronoUnit.HOURS));
        Conversation asBuyer = persistConversation(otherProperty, dealer, now);

        List<Conversation> results = conversationRepository
                .findByBuyerIdOrProperty_DealerIdOrderByLastMessageAtDesc(dealer.getId(), dealer.getId());

        assertThat(results).containsExactly(asBuyer, asDealer);
    }

    @Test
    void findByConversationIdOrderByCreatedAtAsc_returnsPagedOldestFirst() {
        User dealer = persistUser("dealer6", Role.DEALER);
        User buyer = persistUser("buyer6", Role.USER);
        Property property = persistProperty("Villa", dealer);
        Conversation conversation = persistConversation(property, buyer, Instant.now());

        persistMessage(conversation, buyer, "First", null);
        persistMessage(conversation, dealer, "Second", null);
        persistMessage(conversation, buyer, "Third", null);

        Page<Message> page = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId(), PageRequest.of(0, 2, Sort.unsorted()));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Message::getContent).containsExactly("First", "Second");
    }

    @Test
    void countByConversationIdAndSenderIdNotAndReadAtIsNull_countsOnlyUnreadFromOtherSender() {
        User dealer = persistUser("dealer7", Role.DEALER);
        User buyer = persistUser("buyer7", Role.USER);
        Property property = persistProperty("Villa", dealer);
        Conversation conversation = persistConversation(property, buyer, Instant.now());

        persistMessage(conversation, dealer, "Unread from dealer", null);
        persistMessage(conversation, dealer, "Already read", Instant.now());
        persistMessage(conversation, buyer, "My own message", null);

        long unreadForBuyer = messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), buyer.getId());

        assertThat(unreadForBuyer).isEqualTo(1);
    }

    @Test
    void findByConversationIdAndSenderIdNotAndReadAtIsNull_returnsOnlyUnreadFromOtherSender() {
        User dealer = persistUser("dealer8", Role.DEALER);
        User buyer = persistUser("buyer8", Role.USER);
        Property property = persistProperty("Villa", dealer);
        Conversation conversation = persistConversation(property, buyer, Instant.now());

        Message unread = persistMessage(conversation, dealer, "Unread from dealer", null);
        persistMessage(conversation, buyer, "My own unread message", null);

        List<Message> results = messageRepository
                .findByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), buyer.getId());

        assertThat(results).containsExactly(unread);
    }
}
