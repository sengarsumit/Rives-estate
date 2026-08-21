package com.example.estate.Rives.estate.model;

import com.example.estate.Rives.estate.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.UUID;

/**
 * User <-> Property is a bidirectional JPA relationship. Both entities use
 * Lombok @Data, which by default generates equals/hashCode/toString over
 * every field. Without excluding the back-reference on one side, populating
 * both directions in memory recurses forever (StackOverflowError). This test
 * fails loudly (hang/OOM/SOE) if that exclusion regresses.
 */
class EntityCycleSafetyTest {

    @Test
    @Timeout(5)
    void toStringEqualsHashCode_doNotStackOverflow_whenRelationshipIsBidirectional() {
        User dealer = new User();
        dealer.setId(UUID.randomUUID());
        dealer.setUsername("dealerbob");
        dealer.setEmail("bob@dealers.com");
        dealer.setRole(Role.DEALER);

        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Villa");
        property.setAddress("123 Beach Rd");
        property.setDealer(dealer);

        dealer.setProperties(List.of(property));

        dealer.toString();
        property.toString();
        dealer.equals(new User());
        property.equals(new Property());
        dealer.hashCode();
        property.hashCode();
    }
}
