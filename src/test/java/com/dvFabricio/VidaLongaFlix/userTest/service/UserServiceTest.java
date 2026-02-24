package com.dvFabricio.VidaLongaFlix.userTest.service;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.address.Address;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.UserService;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private WelcomeService welcomeService;

    private User user;
    private UUID userId;
    private UserRequestDTO validRequest;
    private Address address;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        address = new Address();

        user = new User("João Silva", "joao@example.com", "encodedPassword",
                "123.456.789-00", "(11) 99999-9999", address);
        ReflectionTestUtils.setField(user, "id", userId);

        validRequest = new UserRequestDTO(
                "João Silva", "joao@example.com", "Password1@",
                "123.456.789-00", "(11) 99999-9999", address);
    }

    @Test
    void shouldFindUserById() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        UserDTO result = userService.findUserById(userId);

        assertEquals("João Silva", result.name());
        assertEquals("joao@example.com", result.email());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> userService.findUserById(userId));
    }

    @Test
    void shouldCreateUser() {
        given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
        given(userRepository.existsByTaxId(validRequest.taxId())).willReturn(false);
        given(passwordEncoder.encode(validRequest.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);
        doNothing().when(welcomeService).sendWelcomeMessage(any(), any());

        UserDTO result = userService.createUser(validRequest);

        assertNotNull(result);
        assertEquals("joao@example.com", result.email());
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailDuplicated() {
        given(userRepository.existsByEmail(validRequest.email())).willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(validRequest));
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowWhenTaxIdDuplicated() {
        given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
        given(userRepository.existsByTaxId(validRequest.taxId())).willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(validRequest));
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
        UserRequestDTO request = new UserRequestDTO(
                "", "joao@example.com", "Password1@",
                "123.456.789-00", "(11) 99999-9999", address);

        assertThrows(MissingRequiredFieldException.class,
                () -> userService.createUser(request));
    }

    @Test
    void shouldThrowWhenAddressIsNull() {
        UserRequestDTO request = new UserRequestDTO(
                "João Silva", "joao@example.com", "Password1@",
                "123.456.789-00", "(11) 99999-9999", null);

        assertThrows(MissingRequiredFieldException.class,
                () -> userService.createUser(request));
    }

    @Test
    void shouldUpdateUser() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("Password1@")).willReturn("newEncoded");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserRequestDTO updateRequest = new UserRequestDTO(
                "Nome Novo", "novo@example.com", "Password1@",
                "123.456.789-00", "(11) 99999-9999", address);

        UserDTO result = userService.updateUser(userId, updateRequest);

        assertEquals("Nome Novo", result.name());
        assertEquals("novo@example.com", result.email());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentUser() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> userService.updateUser(userId, validRequest));
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldDeleteUser() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.deleteUser(userId));
        then(userRepository).should().delete(user);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> userService.deleteUser(userId));
        then(userRepository).should(never()).delete(any());
    }
}