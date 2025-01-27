package com.dvFabricio.VidaLongaFlix.userTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.UserController;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTestMockMVC {

    private MockMvc mockMvc;

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void findAllUsers_ShouldReturnListOfUsers() throws Exception {
        List<UserDTO> userList = List.of(new UserDTO(UUID.randomUUID(), "login1", "email1@test.com", List.of("ROLE_USER"), "password1"));
        when(userService.findAllUsers()).thenReturn(userList);

        mockMvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(1)).andExpect(jsonPath("$[0].login").value("login1")).andExpect(jsonPath("$[0].email").value("email1@test.com"));

        verify(userService).findAllUsers();
    }

    @Test
    void findUserById_ShouldReturnUser_WhenValidId() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(userId, "login1", "email1@test.com", List.of("ROLE_USER"), "password1");
        when(userService.findUserById(userId)).thenReturn(userDTO);

        mockMvc.perform(get("/users/{id}", userId.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.login").value("login1")).andExpect(jsonPath("$.email").value("email1@test.com"));

        verify(userService).findUserById(userId);
    }

    @Test
    void findUserById_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        mockMvc.perform(get("/users/{id}", "invalid-id")).andExpect(status().isBadRequest()).andExpect(content().string("Invalid ID format"));
    }

    @Test
    void createUser_ShouldReturnCreatedUser_WhenValidInput() throws Exception {
        UserRequestDTO request = new UserRequestDTO("login1", "email1@test.com", "password1");
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), "login1", "email1@test.com", List.of("ROLE_USER"), "password1");
        when(userService.createUser(request)).thenReturn(userDTO);

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"login1\",\"email\":\"email1@test.com\",\"password\":\"password1\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.login").value("login1")).andExpect(jsonPath("$.email").value("email1@test.com"));

        verify(userService).createUser(request);
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenDuplicateEmail() throws Exception {
        UserRequestDTO request = new UserRequestDTO("login1", "email1@test.com", "password1");

        when(userService.createUser(request)).thenThrow(new DuplicateResourceException("email", "A user with this email already exists."));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"login1\",\"email\":\"email1@test.com\",\"password\":\"password1\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.field").value("email")).andExpect(jsonPath("$.message").value("A user with this email already exists.")); // Validar a mensagem

        verify(userService).createUser(request);
    }


    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenValidInput() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRequestDTO request = new UserRequestDTO("login2", "email2@test.com", "password2");
        UserDTO userDTO = new UserDTO(userId, "login2", "email2@test.com", List.of("ROLE_USER"), "password2");
        when(userService.updateUser(userId, request)).thenReturn(userDTO);

        mockMvc.perform(put("/users/{id}", userId.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"login2\",\"email\":\"email2@test.com\",\"password\":\"password2\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.login").value("login2")).andExpect(jsonPath("$.email").value("email2@test.com"));

        verify(userService).updateUser(userId, request);
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        mockMvc.perform(put("/users/{id}", "invalid-id").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"login2\",\"email\":\"email2@test.com\",\"password\":\"password2\"}")).andExpect(status().isBadRequest()).andExpect(content().string("Invalid ID format"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent_WhenUserDeleted() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/users/{id}", userId.toString())).andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        mockMvc.perform(delete("/users/{id}", "invalid-id")).andExpect(status().isBadRequest()).andExpect(content().string("Invalid ID format"));
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();

        doThrow(new ResourceNotFoundExceptions("User not found with id: " + userId)).when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId.toString())).andExpect(status().isNotFound()).andExpect(content().string("User not found with id: " + userId));

        verify(userService).deleteUser(userId);
    }
}

