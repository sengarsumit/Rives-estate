package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        // H2 treats USER as a reserved keyword (it's a built-in function); MySQL does not.
        // Quoting every identifier sidesteps the collision without touching the entity mapping.
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        return entityManager.persistAndFlush(user);
    }

    @Test
    void findByEmail_returnsUser_whenEmailExists() {
        persistUser("alice", "alice@example.com");

        Optional<User> result = userRepository.findByEmail("alice@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByEmail_returnsEmptyOptional_whenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUsername_returnsUser_whenUsernameExists() {
        persistUser("bob", "bob@example.com");

        User result = userRepository.findByUsername("bob");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void findByUsername_returnsNull_whenUsernameDoesNotExist() {
        User result = userRepository.findByUsername("ghost");

        assertThat(result).isNull();
    }

    @Test
    void existsByUsername_returnsTrue_whenUsernameExists() {
        persistUser("carol", "carol@example.com");

        assertThat(userRepository.existsByUsername("carol")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenUsernameDoesNotExist() {
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailExists() {
        persistUser("dave", "dave@example.com");

        assertThat(userRepository.existsByEmail("dave@example.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenEmailDoesNotExist() {
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}
