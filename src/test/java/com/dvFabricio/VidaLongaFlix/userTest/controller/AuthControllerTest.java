//package com.dvFabricio.VidaLongaFlix.userTest.controller;
//
//import com.dvFabricio.VidaLongaFlix.controllers.AuthController;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.LoginRequestDTO;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.LoginResponseDTO;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.RegisterRequestDTO;
//import com.dvFabricio.VidaLongaFlix.domain.user.Role;
//import com.dvFabricio.VidaLongaFlix.domain.user.User;
//import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
//import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
//import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@ExtendWith(MockitoExtension.class)
//class AuthControllerTest {
//
//    @InjectMocks
//    private AuthController authController;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private RoleRepository roleRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//    private TokenService tokenService;
//
//    @Test
//    void login_ShouldReturnToken_WhenValidCredentials() {
//        String email = "user@example.com";
//        String password = "password123";
//        String encodedPassword = "encodedPassword123";
//        String token = "mockToken";
//        User user = new User("userLogin", email, encodedPassword);
//
//        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
//        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
//        Mockito.when(tokenService.generateToken(user)).thenReturn(token);
//
//        LoginRequestDTO request = new LoginRequestDTO(email, password);
//        ResponseEntity<?> response = authController.login(request);
//
//        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
//        LoginResponseDTO responseBody = (LoginResponseDTO) response.getBody();
//        Assertions.assertNotNull(responseBody);
//        Assertions.assertEquals("userLogin", responseBody.login());
//        Assertions.assertEquals(token, responseBody.token());
//    }
//
//    @Test
//    void login_ShouldReturnBadRequest_WhenEmailIsEmpty() {
//        LoginRequestDTO request = new LoginRequestDTO("", "password123");
//
//        ResponseEntity<?> response = authController.login(request);
//
//        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        Assertions.assertEquals("Email cannot be empty.", response.getBody());
//    }
//
//    @Test
//    void login_ShouldReturnNotFound_WhenUserDoesNotExist() {
//        String email = "user@example.com";
//
//        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
//
//        LoginRequestDTO request = new LoginRequestDTO(email, "password123");
//        ResponseEntity<?> response = authController.login(request);
//
//        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//        Assertions.assertEquals("User not found", response.getBody());
//    }
//
//    @Test
//    void login_ShouldReturnUnauthorized_WhenPasswordIsInvalid() {
//        String email = "user@example.com";
//        String password = "password123";
//        String encodedPassword = "encodedPassword123";
//        User user = new User("userLogin", email, encodedPassword);
//
//        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
//        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);
//
//        LoginRequestDTO request = new LoginRequestDTO(email, password);
//        ResponseEntity<?> response = authController.login(request);
//
//        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
//        Assertions.assertEquals("Invalid credentials", response.getBody());
//    }
//
//    @Test
//    void register_ShouldCreateUser_WhenValidInput() {
//        String email = "newuser@example.com";
//        String password = "password123";
//        String encodedPassword = "encodedPassword123";
//        String login = "newUser";
//        String token = "mockToken";
//
//        Role role = new Role("ROLE_USER");
//        role.setId(UUID.randomUUID());
//
//        User newUser = new User(login, email, encodedPassword);
//        newUser.setId(UUID.randomUUID());
//
//        Mockito.when(userRepository.existsByEmail(email)).thenReturn(false);
//        Mockito.when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
//        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
//        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
//            User savedUser = invocation.getArgument(0);
//            savedUser.setId(UUID.randomUUID());
//            return savedUser;
//        });
//        Mockito.when(tokenService.generateToken(Mockito.any(User.class))).thenReturn(token);
//
//        RegisterRequestDTO request = new RegisterRequestDTO(login, email, password);
//
//        ResponseEntity<?> response = authController.register(request);
//
//        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        LoginResponseDTO responseBody = (LoginResponseDTO) response.getBody();
//        Assertions.assertNotNull(responseBody);
//        Assertions.assertEquals(login, responseBody.login());
//        Assertions.assertEquals(token, responseBody.token());
//    }
//
//    @Test
//    void register_ShouldReturnBadRequest_WhenEmailAlreadyExists() {
//        String email = "existinguser@example.com";
//        Mockito.when(userRepository.existsByEmail(email)).thenReturn(true);
//
//        RegisterRequestDTO request = new RegisterRequestDTO("login", email, "password123");
//        ResponseEntity<?> response = authController.register(request);
//
//        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        Assertions.assertEquals("Email is already in use.", response.getBody());
//    }
//
//    @Test
//    void register_ShouldReturnInternalServerError_WhenRoleNotFound() {
//        String email = "newuser@example.com";
//        String password = "password123";
//        String login = "newUser";
//
//        Mockito.when(userRepository.existsByEmail(email)).thenReturn(false);
//        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
//
//        RegisterRequestDTO request = new RegisterRequestDTO(login, email, password);
//        ResponseEntity<?> response = authController.register(request);
//
//        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        Assertions.assertEquals("Role 'ROLE_USER' not found", response.getBody());
//    }
//}