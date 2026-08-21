package com.example.estate.Rives.estate.model;

import com.example.estate.Rives.estate.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserJsonTest {

    @Test
    void serializingUser_neverIncludesPassword() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("super-secret-hash");
        user.setRole(Role.USER);

        String json = new ObjectMapper().writeValueAsString(user);

        assertThat(json).doesNotContain("super-secret-hash");
        assertThat(json).doesNotContain("\"password\"");
    }
}
