package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.PropertyCreateDTO;
import com.example.estate.Rives.estate.DTO.PropertyUpdateDTO;
import com.example.estate.Rives.estate.DTO.config.PropertyMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.AuthEntryPointJwt;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.service.ImageService;
import com.example.estate.Rives.estate.service.PropertyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyController.class)
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private ImageService imageService;

    @MockBean
    private PropertyMapper propertyMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    private static User user(String username, Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setRole(role);
        return u;
    }

    private static Property property(UUID id, User dealer) {
        Property p = new Property();
        p.setId(id);
        p.setTitle("Sea View Villa");
        p.setAddress("123 Beach Rd");
        p.setDealer(dealer);
        return p;
    }

    private static Authentication asUser(User principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name()));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void createProperty_success_returns201() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        PropertyCreateDTO dto = new PropertyCreateDTO();
        dto.setTitle("Sea View Villa");
        dto.setAddress("123 Beach Rd");

        when(propertyService.isPropertyExistByTitle("Sea View Villa")).thenReturn(false);
        when(propertyService.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/properties/create")
                        .with(csrf())
                        .with(authentication(asUser(dealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void createProperty_duplicateTitle_returns409() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        PropertyCreateDTO dto = new PropertyCreateDTO();
        dto.setTitle("Sea View Villa");
        dto.setAddress("123 Beach Rd");

        when(propertyService.isPropertyExistByTitle("Sea View Villa")).thenReturn(true);

        mockMvc.perform(post("/properties/create")
                        .with(csrf())
                        .with(authentication(asUser(dealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void createProperty_blankTitle_returns400() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        PropertyCreateDTO dto = new PropertyCreateDTO();
        dto.setTitle("");
        dto.setAddress("123 Beach Rd");

        mockMvc.perform(post("/properties/create")
                        .with(csrf())
                        .with(authentication(asUser(dealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProperty_unknownId_returns404() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        UUID id = UUID.randomUUID();
        when(propertyService.getPropertyById(id)).thenThrow(new ResourceNotFoundException("Property not found with id: " + id));

        mockMvc.perform(delete("/properties/delete/" + id).with(csrf()).with(authentication(asUser(dealer))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProperty_nonOwner_returns403() throws Exception {
        User owner = user("dealerbob", Role.DEALER);
        User otherDealer = user("dealercarl", Role.DEALER);
        UUID id = UUID.randomUUID();
        when(propertyService.getPropertyById(id)).thenReturn(property(id, owner));

        mockMvc.perform(delete("/properties/delete/" + id).with(csrf()).with(authentication(asUser(otherDealer))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProperty_owner_returns200() throws Exception {
        User owner = user("dealerbob", Role.DEALER);
        UUID id = UUID.randomUUID();
        Property existing = property(id, owner);
        when(propertyService.getPropertyById(id)).thenReturn(existing);

        mockMvc.perform(delete("/properties/delete/" + id).with(csrf()).with(authentication(asUser(owner))))
                .andExpect(status().isOk());

        verify(propertyService).delete(existing);
    }

    @Test
    void updateProperty_unknownId_returns404() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        UUID id = UUID.randomUUID();
        when(propertyService.getPropertyById(id)).thenThrow(new ResourceNotFoundException("Property not found with id: " + id));

        mockMvc.perform(patch("/properties/" + id)
                        .with(csrf())
                        .with(authentication(asUser(dealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PropertyUpdateDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProperty_nonOwner_returns403() throws Exception {
        User owner = user("dealerbob", Role.DEALER);
        User otherDealer = user("dealercarl", Role.DEALER);
        UUID id = UUID.randomUUID();
        when(propertyService.getPropertyById(id)).thenReturn(property(id, owner));

        mockMvc.perform(patch("/properties/" + id)
                        .with(csrf())
                        .with(authentication(asUser(otherDealer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PropertyUpdateDTO())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProperty_owner_appliesFieldsAndReturns200() throws Exception {
        User owner = user("dealerbob", Role.DEALER);
        UUID id = UUID.randomUUID();
        Property existing = property(id, owner);
        when(propertyService.getPropertyById(id)).thenReturn(existing);
        when(propertyService.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyUpdateDTO dto = new PropertyUpdateDTO();
        dto.setRental(75000.0);

        mockMvc.perform(patch("/properties/" + id)
                        .with(csrf())
                        .with(authentication(asUser(owner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(propertyService).save(argThatRentalIs(75000.0));
    }

    private static Property argThatRentalIs(double expected) {
        return org.mockito.ArgumentMatchers.argThat(p -> p.getRental() != null && p.getRental() == expected);
    }

    @Test
    void getAllProperty_dealerRole_isNoLongerBlocked() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        when(propertyService.findAllProperties()).thenReturn(List.of());

        mockMvc.perform(get("/properties/all").with(authentication(asUser(dealer))))
                .andExpect(status().isOk());
    }

    @Test
    void searchByLocality_sortsByTitleInRequestedDirection() throws Exception {
        User caller = user("alice", Role.USER);
        when(propertyService.searchByLocality(eq("Goa"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/properties/search/locality")
                        .param("locality", "Goa")
                        .param("sortDir", "asc")
                        .with(authentication(asUser(caller))))
                .andExpect(status().isOk());

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(propertyService).searchByLocality(eq("Goa"), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("title");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void uploadImages_unknownProperty_returns404() throws Exception {
        User dealer = user("dealerbob", Role.DEALER);
        UUID id = UUID.randomUUID();
        when(propertyService.getPropertyById(id)).thenThrow(new ResourceNotFoundException("Property not found with id: " + id));

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "images", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-bytes".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/properties/" + id + "/upload-images")
                        .file(file)
                        .with(csrf())
                        .with(authentication(asUser(dealer))))
                .andExpect(status().isNotFound());
    }
}
