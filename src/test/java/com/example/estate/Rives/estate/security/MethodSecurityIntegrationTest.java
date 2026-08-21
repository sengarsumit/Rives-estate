package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application-context check that @PreAuthorize role checks (as opposed
 * to the manual ownership `if`s in the controllers) are actually enforced by
 * the real WebSecurityConfig/@EnableMethodSecurity wiring - not just an
 * artifact of a @WebMvcTest slice's bean set. This is deliberately a full
 * @SpringBootTest, not a slice, so there is no ambiguity about which
 * SecurityFilterChain/method-security configuration is active.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MethodSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static User user(String username, Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setRole(role);
        return u;
    }

    private static Authentication asUser(User principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name()));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void dealerOnlyEndpoint_rejectsAuthenticatedUserWithWrongRole() throws Exception {
        User plainUser = user("alice", Role.USER);

        mockMvc.perform(post("/properties/create")
                        .with(csrf())
                        .with(authentication(asUser(plainUser)))
                        .contentType("application/json")
                        .content("{\"title\":\"x\",\"address\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpoint_rejectsAuthenticatedUserWithWrongRole() throws Exception {
        User plainUser = user("alice", Role.USER);

        mockMvc.perform(get("/api/v1/users/all").with(authentication(asUser(plainUser))))
                .andExpect(status().isForbidden());
    }
}
