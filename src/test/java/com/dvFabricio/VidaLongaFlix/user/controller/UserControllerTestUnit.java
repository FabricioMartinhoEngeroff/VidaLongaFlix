package com.dvFabricio.VidaLongaFlix.user.controller;

import com.dvFabricio.VidaLongaFlix.controllers.UserController;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class UserControllerTestUnit {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Test
    void findAllUsers_ShouldReturnListOfUsers() {
        List<UserDTO> userList = List.of(new UserDTO(UUID.randomUUID(), "login1", "email1@test.com", List.of("ROLE_USER"), "password1"));
        Mockito.when(userService.findAllUsers()).thenReturn(userList);

        ResponseEntity<List<UserDTO>> response = userController.findAllUsers();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(userList, response.getBody());
    }

    @Test
    void findUserById_ShouldReturnUser_WhenValidId() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(userId, "login1", "email1@test.com", List.of("ROLE_USER"), "password1");
        Mockito.when(userService.findUserById(userId)).thenReturn(userDTO);

        ResponseEntity<?> response = userController.findUserById(userId.toString());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(userDTO, response.getBody());
    }

    @Test
    void findUserById_ShouldReturnBadRequest_WhenInvalidId() {
        ResponseEntity<?> response = userController.findUserById("invalid-id");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Invalid ID format", response.getBody());
    }

    @Test
    void findUserById_ShouldReturnNotFound_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        Mockito.when(userService.findUserById(userId)).thenThrow(new ResourceNotFoundExceptions("User not found"));

        ResponseEntity<?> response = userController.findUserById(userId.toString());

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertEquals("User not found", response.getBody());
    }

    @Test
    void createUser_ShouldReturnCreatedUser_WhenValidInput() {
        UserRequestDTO request = new UserRequestDTO("login1", "email1@test.com", "password1");
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), "login1", "email1@test.com", List.of("ROLE_USER"), "password1");
        Mockito.when(userService.createUser(request)).thenReturn(userDTO);

        ResponseEntity<UserDTO> response = userController.createUser(request);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertEquals(userDTO, response.getBody());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenDuplicateEmail() {
        UserRequestDTO request = new UserRequestDTO("login1", "email1@test.com", "password1");
        Mockito.when(userService.createUser(request)).thenThrow(new DuplicateResourceException("email", "Email is already in use."));

        ResponseEntity<UserDTO> response = userController.createUser(request);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenValidInput() {
        UUID userId = UUID.randomUUID();
        UserRequestDTO request = new UserRequestDTO("login2", "email2@test.com", "password2");
        UserDTO userDTO = new UserDTO(userId, "login2", "email2@test.com", List.of("ROLE_USER"), "password2");
        Mockito.when(userService.updateUser(userId, request)).thenReturn(userDTO);

        ResponseEntity<?> response = userController.updateUser(userId.toString(), request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(userDTO, response.getBody());
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenInvalidId() {
        UserRequestDTO request = new UserRequestDTO("login2", "email2@test.com", "password2");

        ResponseEntity<?> response = userController.updateUser("invalid-id", request);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Invalid ID format", response.getBody());
    }

    @Test
    void deleteUser_ShouldReturnNoContent_WhenUserDeleted() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<?> response = userController.deleteUser(userId.toString());

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Mockito.verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_ShouldReturnBadRequest_WhenInvalidId() {
        ResponseEntity<?> response = userController.deleteUser("invalid-id");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Invalid ID format", response.getBody());
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        Mockito.doThrow(new ResourceNotFoundExceptions("User not found")).when(userService).deleteUser(userId);

        ResponseEntity<?> response = userController.deleteUser(userId.toString());

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertEquals("User not found", response.getBody());
    }

}
