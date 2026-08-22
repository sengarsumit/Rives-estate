package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UpdateUserDTO;
import com.example.estate.Rives.estate.DTO.UserResponseDTO;
import com.example.estate.Rives.estate.DTO.config.UserMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.security.AuthEntryPointJwt;
import com.example.estate.Rives.estate.security.WebSecurityConfig;
import com.example.estate.Rives.estate.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for authorization/ownership around user delete & update, running
 * against the real WebSecurityConfig (explicitly @Import-ed - @WebMvcTest's
 * automatic retention of security @Configuration classes is inconsistent, so
 * @PreAuthorize is only reliably enforced here because of that explicit
 * import). No live DB required: JwtUtil/UserRepository are mocked since
 * Authentication is injected directly per-request, bypassing JWT parsing.
 */
@WebMvcTest(UserController.class)
@Import({WebSecurityConfig.class, AuthEntryPointJwt.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserMapper userMapper;

    // AuthEntryPointJwt is intentionally NOT mocked: it's what actually writes
    // the 401 status for unauthenticated requests. A Mockito no-op mock here
    // would silently swallow that write and leave the response at 200 - it
    // has zero dependencies, so using the real bean is free.

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

    // ---- DELETE /api/v1/users/{username} ----

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/alice").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_self_returns200AndDeletes() throws Exception {
        User alice = user("alice", Role.USER);
        when(userService.isUsernameExist("alice")).thenReturn(true);
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        mockMvc.perform(delete("/api/v1/users/alice").with(csrf()).with(authentication(asUser(alice))))
                .andExpect(status().isOk());

        verify(userService).deleteUser(alice);
    }

    @Test
    void deleteUser_differentNonAdminUser_returns403() throws Exception {
        User bob = user("bob", Role.USER);

        mockMvc.perform(delete("/api/v1/users/alice").with(csrf()).with(authentication(asUser(bob))))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUser_adminDeletingOtherUser_returns200() throws Exception {
        User admin = user("root", Role.ADMIN);
        User target = user("alice", Role.USER);
        when(userService.isUsernameExist("alice")).thenReturn(true);
        when(userService.getUserByUsername("alice")).thenReturn(target);

        mockMvc.perform(delete("/api/v1/users/alice").with(csrf()).with(authentication(asUser(admin))))
                .andExpect(status().isOk());

        verify(userService).deleteUser(target);
    }

    @Test
    void deleteUser_unknownUsername_returns404() throws Exception {
        User admin = user("root", Role.ADMIN);
        when(userService.isUsernameExist("ghost")).thenReturn(false);

        mockMvc.perform(delete("/api/v1/users/ghost").with(csrf()).with(authentication(asUser(admin))))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH /api/v1/users/{username} ----

    @Test
    void updateUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserDTO())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_self_updatesAllowedFields() throws Exception {
        User alice = user("alice", Role.USER);
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setFirstName("Alicia");

        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .with(authentication(asUser(alice)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).updateUser(argThat(u -> "Alicia".equals(u.getFirstName())));
    }

    @Test
    void updateUser_differentNonAdminUser_returns403() throws Exception {
        User bob = user("bob", Role.USER);

        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .with(authentication(asUser(bob)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserDTO())))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(any());
    }

    @Test
    void updateUser_unknownUsername_returns404() throws Exception {
        User admin = user("root", Role.ADMIN);
        when(userService.getUserByUsername("ghost")).thenReturn(null);

        mockMvc.perform(patch("/api/v1/users/ghost")
                        .with(csrf())
                        .with(authentication(asUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_selfAttemptsRoleChange_roleIsIgnored() throws Exception {
        User alice = user("alice", Role.USER);
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .with(authentication(asUser(alice)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).updateUser(argThat(u -> u.getRole() == Role.USER));
    }

    @Test
    void updateUser_adminChangesOtherUsersRole_roleIsApplied() throws Exception {
        User admin = user("root", Role.ADMIN);
        User bob = user("bob", Role.USER);
        when(userService.getUserByUsername("bob")).thenReturn(bob);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setRole(Role.DEALER);

        mockMvc.perform(patch("/api/v1/users/bob")
                        .with(csrf())
                        .with(authentication(asUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).updateUser(argThat(u -> u.getRole() == Role.DEALER));
    }

    @Test
    void updateUser_success_returnsUserResponseDtoWithoutPassword() throws Exception {
        User alice = user("alice", Role.USER);
        alice.setFirstName("Alicia");
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        UserResponseDTO responseDto = new UserResponseDTO();
        responseDto.setId(alice.getId());
        responseDto.setUsername("alice");
        responseDto.setEmail(alice.getEmail());
        responseDto.setRole(Role.USER);
        responseDto.setFirstName("Alicia");
        when(userMapper.userToDto(alice)).thenReturn(responseDto);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setFirstName("Alicia");

        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .with(authentication(asUser(alice)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alicia"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updateUser_passwordProvided_isEncodedBeforeSave() throws Exception {
        User alice = user("alice", Role.USER);
        when(userService.getUserByUsername("alice")).thenReturn(alice);
        when(passwordEncoder.encode("newpassword123")).thenReturn("ENCODED_HASH");

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setPassword("newpassword123");

        mockMvc.perform(patch("/api/v1/users/alice")
                        .with(csrf())
                        .with(authentication(asUser(alice)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).updateUser(argThat(u -> "ENCODED_HASH".equals(u.getPassword())));
    }
}
