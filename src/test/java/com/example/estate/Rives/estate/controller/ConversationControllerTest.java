package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.ConversationCreateDTO;
import com.example.estate.Rives.estate.DTO.ConversationDTO;
import com.example.estate.Rives.estate.DTO.ConversationSummaryDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.config.ChatMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.AuthEntryPointJwt;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.security.WebSecurityConfig;
import com.example.estate.Rives.estate.security.WsTicketService;
import com.example.estate.Rives.estate.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@Import({WebSecurityConfig.class, AuthEntryPointJwt.class})
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ChatMapper chatMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WsTicketService wsTicketService;

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
    void createOrGetConversation_success_returns200WithConversation() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID propertyId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());

        ConversationCreateDTO dto = new ConversationCreateDTO();
        dto.setPropertyId(propertyId);

        ConversationDTO responseDto = new ConversationDTO();
        responseDto.setId(conversation.getId());
        responseDto.setPropertyId(propertyId);

        when(chatService.getOrCreateConversation(propertyId, buyer)).thenReturn(conversation);
        when(chatMapper.conversationToDto(conversation)).thenReturn(responseDto);

        mockMvc.perform(post("/conversations")
                        .with(csrf())
                        .with(authentication(asUser(buyer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyId").value(propertyId.toString()));
    }

    @Test
    void createOrGetConversation_missingPropertyId_returns400() throws Exception {
        User buyer = user("alice", Role.USER);

        mockMvc.perform(post("/conversations")
                        .with(csrf())
                        .with(authentication(asUser(buyer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrGetConversation_ownProperty_returns400() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        UUID propertyId = UUID.randomUUID();
        ConversationCreateDTO dto = new ConversationCreateDTO();
        dto.setPropertyId(propertyId);

        when(chatService.getOrCreateConversation(propertyId, dealer))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "You cannot start a conversation about your own property"));

        mockMvc.perform(post("/conversations")
                        .with(csrf())
                        .with(authentication(asUser(dealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrGetConversation_unknownProperty_returns404() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID propertyId = UUID.randomUUID();
        ConversationCreateDTO dto = new ConversationCreateDTO();
        dto.setPropertyId(propertyId);

        when(chatService.getOrCreateConversation(propertyId, buyer))
                .thenThrow(new ResourceNotFoundException("Property not found with id: " + propertyId));

        mockMvc.perform(post("/conversations")
                        .with(csrf())
                        .with(authentication(asUser(buyer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrGetConversation_anonymous_returns401() throws Exception {
        ConversationCreateDTO dto = new ConversationCreateDTO();
        dto.setPropertyId(UUID.randomUUID());

        mockMvc.perform(post("/conversations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyConversations_success_returns200() throws Exception {
        User buyer = user("alice", Role.USER);
        when(chatService.getConversationsForUser(buyer)).thenReturn(List.of(new ConversationSummaryDTO()));

        mockMvc.perform(get("/conversations").with(authentication(asUser(buyer))))
                .andExpect(status().isOk());
    }

    @Test
    void getMyConversations_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getConversation_participant_returns200() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversationId);

        when(chatService.getConversation(conversationId, buyer)).thenReturn(conversation);
        when(chatMapper.conversationToDto(conversation)).thenReturn(dto);

        mockMvc.perform(get("/conversations/" + conversationId).with(authentication(asUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }

    @Test
    void getConversation_nonParticipant_returns403() throws Exception {
        User stranger = user("carl", Role.USER);
        UUID conversationId = UUID.randomUUID();

        when(chatService.getConversation(conversationId, stranger))
                .thenThrow(new AccessDeniedException("You are not a participant in this conversation"));

        mockMvc.perform(get("/conversations/" + conversationId).with(authentication(asUser(stranger))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConversation_unknown_returns404() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID conversationId = UUID.randomUUID();

        when(chatService.getConversation(conversationId, buyer))
                .thenThrow(new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        mockMvc.perform(get("/conversations/" + conversationId).with(authentication(asUser(buyer))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConversation_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/conversations/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMessages_participant_returns200() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID conversationId = UUID.randomUUID();
        MessageDTO message = new MessageDTO();
        message.setContent("Hello");
        Page<MessageDTO> page = new PageImpl<>(List.of(message));

        when(chatService.getMessages(eq(conversationId), eq(buyer), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/conversations/" + conversationId + "/messages").with(authentication(asUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hello"));
    }

    @Test
    void getMessages_nonParticipant_returns403() throws Exception {
        User stranger = user("carl", Role.USER);
        UUID conversationId = UUID.randomUUID();

        when(chatService.getMessages(eq(conversationId), eq(stranger), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("You are not a participant in this conversation"));

        mockMvc.perform(get("/conversations/" + conversationId + "/messages").with(authentication(asUser(stranger))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMessages_unknownConversation_returns404() throws Exception {
        User buyer = user("alice", Role.USER);
        UUID conversationId = UUID.randomUUID();

        when(chatService.getMessages(eq(conversationId), eq(buyer), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        mockMvc.perform(get("/conversations/" + conversationId + "/messages").with(authentication(asUser(buyer))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessages_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/conversations/" + UUID.randomUUID() + "/messages"))
                .andExpect(status().isUnauthorized());
    }
}
