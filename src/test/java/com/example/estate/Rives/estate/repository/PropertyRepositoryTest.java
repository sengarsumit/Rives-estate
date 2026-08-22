package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.config.JpaAuditingConfig;
import com.example.estate.Rives.estate.enums.Role;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PropertyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PropertyRepository propertyRepository;

    private User persistDealer(String username) {
        User dealer = new User();
        dealer.setUsername(username);
        dealer.setEmail(username + "@example.com");
        dealer.setPassword("password123");
        dealer.setRole(Role.DEALER);
        return entityManager.persistAndFlush(dealer);
    }

    private Property persistProperty(String title, String locality, User dealer) {
        Property property = new Property();
        property.setTitle(title);
        property.setAddress("123 Main St");
        property.setLocality(locality);
        property.setRental(15000.0);
        property.setDealer(dealer);
        return entityManager.persistAndFlush(property);
    }

    @Test
    void findByLocalityContainingIgnoreCase_listOverload_returnsCaseInsensitiveSubstringMatches() {
        User dealer = persistDealer("dealer1");
        persistProperty("Cozy 2BHK", "HSR Layout", dealer);
        persistProperty("Sunny Studio", "hsr extension", dealer);
        persistProperty("Large Villa", "Indiranagar", dealer);

        List<Property> results = propertyRepository.findByLocalityContainingIgnoreCase("hsr");

        assertThat(results).hasSize(2)
                .extracting(Property::getTitle)
                .containsExactlyInAnyOrder("Cozy 2BHK", "Sunny Studio");
    }

    @Test
    void findByLocalityContainingIgnoreCase_listOverload_returnsEmptyListWhenNoMatch() {
        User dealer = persistDealer("dealer2");
        persistProperty("Cozy 2BHK", "HSR Layout", dealer);

        List<Property> results = propertyRepository.findByLocalityContainingIgnoreCase("Whitefield");

        assertThat(results).isEmpty();
    }

    @Test
    void findByLocalityContainingIgnoreCase_pageableOverload_returnsPagedCaseInsensitiveMatches() {
        User dealer = persistDealer("dealer3");
        persistProperty("Alpha", "HSR Layout", dealer);
        persistProperty("Beta", "hsr extension", dealer);
        persistProperty("Gamma", "HSR BTM", dealer);

        Page<Property> page = propertyRepository.findByLocalityContainingIgnoreCase(
                "hsr", PageRequest.of(0, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.getContent()).extracting(Property::getTitle)
                .containsExactly("Alpha", "Beta");
    }

    @Test
    void findByLocalityContainingIgnoreCase_pageableOverload_secondPageReturnsRemainingResultAndIsLast() {
        User dealer = persistDealer("dealer4");
        persistProperty("Alpha", "HSR Layout", dealer);
        persistProperty("Beta", "hsr extension", dealer);
        persistProperty("Gamma", "HSR BTM", dealer);

        Page<Property> page = propertyRepository.findByLocalityContainingIgnoreCase(
                "hsr", PageRequest.of(1, 2, Sort.by("title")));

        assertThat(page.getContent()).extracting(Property::getTitle).containsExactly("Gamma");
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void findByTitle_returnsProperty_whenTitleExists() {
        User dealer = persistDealer("dealer5");
        persistProperty("Unique Title", "Koramangala", dealer);

        Optional<Property> result = propertyRepository.findByTitle("Unique Title");

        assertThat(result).isPresent();
        assertThat(result.get().getLocality()).isEqualTo("Koramangala");
    }

    @Test
    void findByTitle_returnsEmptyOptional_whenTitleDoesNotExist() {
        Optional<Property> result = propertyRepository.findByTitle("Does Not Exist");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByTitle_returnsTrue_whenTitleExists() {
        User dealer = persistDealer("dealer6");
        persistProperty("Existing Title", "Koramangala", dealer);

        assertThat(propertyRepository.existsByTitle("Existing Title")).isTrue();
    }

    @Test
    void existsByTitle_returnsFalse_whenTitleDoesNotExist() {
        assertThat(propertyRepository.existsByTitle("Nonexistent Title")).isFalse();
    }

    @Test
    void findByDealer_returnsOnlyPropertiesOwnedByThatDealer() {
        User dealerA = persistDealer("dealerA");
        User dealerB = persistDealer("dealerB");
        persistProperty("Dealer A Property", "Koramangala", dealerA);
        persistProperty("Dealer B Property", "Indiranagar", dealerB);

        List<Property> results = propertyRepository.findByDealer(dealerA);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Dealer A Property");
    }

    @Test
    void save_populatesAuditingTimestamps_onCreate() {
        User dealer = persistDealer("dealer7");
        Property property = new Property();
        property.setTitle("Timestamped Property");
        property.setAddress("456 Side St");
        property.setLocality("Whitefield");
        property.setRental(20000.0);
        property.setDealer(dealer);

        Property saved = propertyRepository.saveAndFlush(property);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_rejectsPropertyWithNoDealer() {
        Property property = new Property();
        property.setTitle("Ownerless Property");
        property.setAddress("789 Nowhere Ave");
        property.setLocality("Whitefield");
        property.setRental(12000.0);
        // dealer intentionally left unset - every property must have an owner.

        assertThatThrownBy(() -> propertyRepository.saveAndFlush(property))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
