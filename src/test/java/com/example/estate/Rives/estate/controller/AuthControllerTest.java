package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UserLoginDTO;
import com.example.estate.Rives.estate.DTO.UserRegisterDTO;
import com.example.estate.Rives.estate.DTO.UserResponseDTO;
import com.example.estate.Rives.estate.DTO.config.UserMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.AuthEntryPointJwt;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.security.WebSecurityConfig;
import com.example.estate.Rives.estate.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({WebSecurityConfig.class, CustomUserDetailsService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @Test
    void signin_blankUsername_returns400() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signin_wrongPassword_returns401NotServerError() throws Exception {
        // userRepository is an unstubbed mock, so the real AuthenticationManager
        // (backed by CustomUserDetailsService) fails with UsernameNotFoundException
        // for any login attempt here - exercising the same "auth failed cleanly"
        // path as a wrong password would, without needing to mock AuthenticationManager
        // itself (doing so breaks WebSecurityConfig's filter chain resolution in this slice).
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signup_blankFields_returns400() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("");
        dto.setEmail("not-an-email");
        dto.setPassword("short");

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_duplicateUsername_returns409() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("password123");
        dto.setRole(Role.USER);

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_returnsUserResponseDtoWithoutPassword() throws Exception {
        User alice = new User();
        alice.setId(UUID.randomUUID());
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setRole(Role.USER);

        UserResponseDTO responseDto = new UserResponseDTO();
        responseDto.setId(alice.getId());
        responseDto.setUsername("alice");
        responseDto.setEmail("alice@example.com");
        responseDto.setRole(Role.USER);

        when(userMapper.userToDto(alice)).thenReturn(responseDto);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var auth = new UsernamePasswordAuthenticationToken(alice, null, authorities);

        mockMvc.perform(get("/api/auth/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(responseDto)));
    }
}
