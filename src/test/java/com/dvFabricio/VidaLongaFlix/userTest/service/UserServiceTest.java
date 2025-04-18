//package com.dvFabricio.VidaLongaFlix.userTest.service;
//
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
//import com.dvFabricio.VidaLongaFlix.domain.user.User;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
//import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
//import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
//import com.dvFabricio.VidaLongaFlix.services.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//
//    @InjectMocks
//    private UserService userService;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    private User user;
//    private UserRequestDTO userRequestDTO;
//
//    @BeforeEach
//    void setup() {
//        user = new User("user", "user@example.com", "password");
//        userRequestDTO = new UserRequestDTO("user", "user@example.com", "password");
//    }
//
//    @Test
//    void deveriaBuscarTodosOsUsuarios() {
//        User user1 = new User("user1", "user1@example.com", "password1");
//        User user2 = new User("user2", "user2@example.com", "password2");
//
//        given(userRepository.findAll()).willReturn(List.of(user1, user2));
//
//        List<UserDTO> result = userService.findAllUsers();
//
//        assertAll(
//                () -> assertEquals(2, result.size(), "Deve retornar exatamente 2 usuários"),
//                () -> assertEquals("user1", result.get(0).login(), "Login do primeiro usuário deve ser 'user1'"),
//                () -> assertEquals("user1@example.com", result.get(0).email(), "Email do primeiro usuário deve ser 'user1@example.com'"),
//                () -> assertEquals("user2", result.get(1).login(), "Login do segundo usuário deve ser 'user2'"),
//                () -> assertEquals("user2@example.com", result.get(1).email(), "Email do segundo usuário deve ser 'user2@example.com'")
//        );
//
//        then(userRepository).should().findAll();
//    }
//
//    @Test
//    void deveriaRetornarUsuarioPeloId() {
//        UUID userId = UUID.randomUUID();
//        user.setId(userId);
//        given(userRepository.findById(userId)).willReturn(Optional.of(user));
//
//        UserDTO result = userService.findUserById(userId);
//
//        assertAll(
//                () -> assertNotNull(result, "O resultado não deve ser nulo"),
//                () -> assertEquals("user", result.login(), "Login deve ser 'user'"),
//                () -> assertEquals("user@example.com", result.email(), "Email deve ser 'user@example.com'")
//        );
//
//        then(userRepository).should().findById(userId);
//    }
//
//    @Test
//    void deveriaLancarExcecaoQuandoUsuarioNaoEncontradoPorId() {
//        UUID userId = UUID.randomUUID();
//        given(userRepository.findById(userId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> userService.findUserById(userId)
//        );
//
//        assertEquals("User not found with id: " + userId, exception.getMessage());
//        then(userRepository).should().findById(userId);
//    }
//
//    @Test
//    void deveriaCadastrarNovoUsuario() {
//        given(userRepository.existsByEmail(userRequestDTO.email())).willReturn(false);
//        given(passwordEncoder.encode(userRequestDTO.password())).willReturn("encodedPassword");
//        given(userRepository.save(any(User.class))).willReturn(user);
//
//        UserDTO result = userService.createUser(userRequestDTO);
//
//        assertAll(
//                () -> assertNotNull(result, "O resultado não deve ser nulo"),
//                () -> assertEquals("user@example.com", result.email(), "Email deve ser 'user@example.com'"),
//                () -> assertEquals("user", result.login(), "Login deve ser 'user'")
//        );
//
//        then(userRepository).should().existsByEmail(userRequestDTO.email());
//        then(passwordEncoder).should().encode(userRequestDTO.password());
//        then(userRepository).should().save(any(User.class));
//    }
//
//    @Test
//    void naoDeveriaCadastrarUsuarioComEmailDuplicado() {
//        given(userRepository.existsByEmail(userRequestDTO.email())).willReturn(true);
//
//        DuplicateResourceException exception = assertThrows(
//                DuplicateResourceException.class,
//                () -> userService.createUser(userRequestDTO)
//        );
//
//        assertEquals("Email is already in use.", exception.getMessage());
//        then(userRepository).should().existsByEmail(userRequestDTO.email());
//        then(userRepository).should(never()).save(any(User.class));
//    }
//
//
//    @Test
//    void deveriaLancarExcecaoQuandoCamposObrigatoriosFaltarem() {
//        assertAll(
//                () -> assertThrows(MissingRequiredFieldException.class,
//                        () -> userService.createUser(new UserRequestDTO("", "valid@example.com", "password"))
//                ),
//                () -> assertThrows(MissingRequiredFieldException.class,
//                        () -> userService.createUser(new UserRequestDTO("login", "", "password"))
//                ),
//                () -> assertThrows(MissingRequiredFieldException.class,
//                        () -> userService.createUser(new UserRequestDTO("login", "valid@example.com", ""))
//                )
//        );
//    }
//
//    @Test
//    void deveriaAtualizarUsuario() {
//        UUID userId = UUID.randomUUID();
//        user.setId(userId);
//
//        given(userRepository.findById(userId)).willReturn(Optional.of(user));
//        given(passwordEncoder.encode("updatedPassword")).willReturn("encodedUpdatedPassword");
//        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0, User.class));
//
//        UserRequestDTO updateRequest = new UserRequestDTO("updatedUser", "updated@example.com", "updatedPassword");
//
//        UserDTO result = userService.updateUser(userId, updateRequest);
//
//        assertAll(
//                () -> assertNotNull(result, "O resultado não deve ser nulo"),
//                () -> assertEquals("updatedUser", result.login(), "Login deve ser atualizado para 'updatedUser'"),
//                () -> assertEquals("updated@example.com", result.email(), "Email deve ser atualizado para 'updated@example.com'")
//        );
//
//        then(userRepository).should().findById(userId);
//        then(passwordEncoder).should().encode("updatedPassword");
//        then(userRepository).should().save(any(User.class));
//    }
//
//    @Test
//    void naoDeveriaAtualizarUsuarioNaoEncontrado() {
//        UUID userId = UUID.randomUUID();
//        given(userRepository.findById(userId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> userService.updateUser(userId, userRequestDTO)
//        );
//
//        assertEquals("User not found with id: " + userId, exception.getMessage());
//        then(userRepository).should().findById(userId);
//        then(userRepository).should(never()).save(any(User.class));
//    }
//
//
//    @Test
//    void deveriaDeletarUsuarioExistente() {
//        UUID userId = user.getId();
//        given(userRepository.findById(userId)).willReturn(Optional.of(user));
//
//        assertDoesNotThrow(() -> userService.deleteUser(userId));
//
//        then(userRepository).should().findById(userId);
//        then(userRepository).should().delete(user);
//    }
//
//
//    @Test
//    void naoDeveriaDeletarUsuarioNaoEncontrado() {
//        UUID userId = UUID.randomUUID();
//        given(userRepository.findById(userId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> userService.deleteUser(userId)
//        );
//
//        assertEquals("User not found with id: " + userId, exception.getMessage());
//        then(userRepository).should().findById(userId);
//        then(userRepository).should(never()).delete(any(User.class));
//    }
//}