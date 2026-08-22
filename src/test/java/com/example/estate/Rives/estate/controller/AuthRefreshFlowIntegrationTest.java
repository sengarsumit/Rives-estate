package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UserLoginDTO;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full application context against the real (locally configured) database,
// exercising the real AuthenticationManager/BCrypt/CustomUserDetailsService -
// deliberately not a @WebMvcTest slice, since the thing under test is the
// rotation/reuse-detection behavior across signin -> refresh -> refresh
// again, which only means something end-to-end. @Transactional rolls back
// everything this test writes so the shared dev database isn't polluted.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRefreshFlowIntegrationTest {

    private static final String USERNAME = "refreshflowtestuser";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(USERNAME + "@example.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    private MvcResult signin() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(USERNAME);
        dto.setPassword(PASSWORD);

        return mockMvc.perform(post("/api/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String cookieValue(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).as("cookie " + name).isNotNull();
        return cookie.getValue();
    }

    @Test
    void refresh_rotatesBothTokens() throws Exception {
        MvcResult signinResult = signin();
        String originalRefreshToken = cookieValue(signinResult, "refreshToken");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String rotatedRefreshToken = cookieValue(refreshResult, "refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);
        assertThat(refreshResult.getResponse().getCookie("accessToken")).isNotNull();
    }

    @Test
    void refresh_reusingARotatedOutToken_isRejectedAndKillsEverySession() throws Exception {
        MvcResult signinResult = signin();
        String originalRefreshToken = cookieValue(signinResult, "refreshToken");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedRefreshToken = cookieValue(refreshResult, "refreshToken");

        // Reusing the token that was already exchanged is a theft signal.
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andExpect(status().isUnauthorized());

        // The reuse-detection response is to revoke every active session for
        // the user, so even the legitimately-rotated token stops working too.
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withoutHavingSignedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", "not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheRefreshTokenSoItCanNoLongerBeUsed() throws Exception {
        MvcResult signinResult = signin();
        String refreshToken = cookieValue(signinResult, "refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }
}
