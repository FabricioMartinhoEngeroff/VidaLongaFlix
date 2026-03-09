package com.dvFabricio.VidaLongaFlix.userTest.service;

import com.dvFabricio.VidaLongaFlix.domain.auth.QueueLoginErrorDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.config.AppConfig;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.MaxUsersConfigResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistMessageDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistResponseDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.AppConfigRepository;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.RegistrationLimitService;
import com.dvFabricio.VidaLongaFlix.services.WaitlistNotificationService;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationLimitServiceTest {

    @InjectMocks
    private RegistrationLimitService registrationLimitService;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AppConfigRepository appConfigRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private WelcomeService welcomeService;
    @Mock private WaitlistNotificationService waitlistNotificationService;

    private RegisterRequestDTO validRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequestDTO(
                "Usuario Teste",
                "usuario@test.com",
                "Senha@1234",
                "(11) 98765-4321"
        );
        userRole = new Role("ROLE_USER");

        given(appConfigRepository.findByKeyForUpdate(RegistrationLimitService.MAX_ACTIVE_USERS_KEY))
                .willReturn(Optional.of(new AppConfig(RegistrationLimitService.MAX_ACTIVE_USERS_KEY, "2")));
        given(appConfigRepository.findById(RegistrationLimitService.MAX_ACTIVE_USERS_KEY))
                .willReturn(Optional.of(new AppConfig(RegistrationLimitService.MAX_ACTIVE_USERS_KEY, "2")));
        given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(userRole));
        given(passwordEncoder.encode(validRequest.password())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            }
            return user;
        });
    }

    @Test
    void shouldRegisterActiveUserWhenThereIsAvailableCapacity() {
        given(userRepository.findByEmail(validRequest.email())).willReturn(Optional.empty());
        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(1L);
        given(tokenService.generateToken(any(User.class))).willReturn("jwt-token");

        RegistrationResponseDTO response = registrationLimitService.register(validRequest);

        assertFalse(response.queued());
        assertEquals("jwt-token", response.token());
        assertEquals(UserStatus.ACTIVE, response.user().status());
        assertNull(response.queuePosition());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertNull(savedUser.getQueuePosition());
        assertEquals("encoded-password", savedUser.getPassword());

        then(welcomeService).should().sendWelcomeMessage("Usuario Teste", "(11) 98765-4321");
        then(waitlistNotificationService).should(never()).notifyQueued(any(User.class));
    }

    @Test
    void shouldQueueUserWhenActiveLimitIsReached() {
        given(userRepository.findByEmail(validRequest.email())).willReturn(Optional.empty());
        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(2L);
        given(userRepository.findMaxQueuePosition(UserStatus.QUEUED)).willReturn(Optional.of(4));

        RegistrationResponseDTO response = registrationLimitService.register(validRequest);

        assertTrue(response.queued());
        assertNull(response.token());
        assertEquals(5, response.queuePosition());
        assertEquals(UserStatus.QUEUED, response.user().status());
        assertEquals(5, response.user().queuePosition());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.QUEUED, savedUser.getStatus());
        assertEquals(5, savedUser.getQueuePosition());

        then(waitlistNotificationService).should().notifyQueued(any(User.class));
        then(welcomeService).should(never()).sendWelcomeMessage(any(), any());
    }

    @Test
    void shouldRejectDuplicateEmailAlreadyQueued() {
        User queuedUser = new User("Fila", validRequest.email(), "pwd", "(11) 99999-9999");
        queuedUser.setStatus(UserStatus.QUEUED);
        queuedUser.setQueuePosition(3);

        given(userRepository.findByEmail(validRequest.email())).willReturn(Optional.of(queuedUser));

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> registrationLimitService.register(validRequest)
        );

        assertTrue(exception.getMessage().contains("already on the waitlist"));
    }

    @Test
    void shouldBuildQueuedLoginPayloadFromUserState() {
        User queuedUser = new User("Fila", "fila@test.com", "pwd", "(11) 99999-9999");
        queuedUser.setStatus(UserStatus.QUEUED);
        queuedUser.setQueuePosition(7);

        QueueLoginErrorDTO response = registrationLimitService.buildQueuedLoginError(queuedUser);

        assertEquals("ACCOUNT_QUEUED", response.error());
        assertEquals(7, response.queuePosition());
    }

    @Test
    void shouldActivateQueuedUserAndRecalculatePositions() {
        UUID queuedId = UUID.randomUUID();
        User queuedToActivate = new User("Primeiro", "primeiro@test.com", "pwd", "(11) 99999-9999");
        queuedToActivate.setId(queuedId);
        queuedToActivate.setStatus(UserStatus.QUEUED);
        queuedToActivate.setQueuePosition(1);

        User remainingQueued = new User("Segundo", "segundo@test.com", "pwd", "(11) 98888-8888");
        remainingQueued.setId(UUID.randomUUID());
        remainingQueued.setStatus(UserStatus.QUEUED);
        remainingQueued.setQueuePosition(2);

        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(1L);
        given(userRepository.findById(queuedId)).willReturn(Optional.of(queuedToActivate));
        given(userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED))
                .willReturn(List.of(remainingQueued));

        RegistrationResponseDTO response = registrationLimitService.activateQueuedUser(queuedId);

        assertFalse(response.queued());
        assertEquals(UserStatus.ACTIVE, response.user().status());
        assertNull(response.user().queuePosition());
        assertEquals(1, remainingQueued.getQueuePosition());

        then(waitlistNotificationService).should().notifyActivated(queuedToActivate);
        then(userRepository).should().saveAll(List.of(remainingQueued));
    }

    @Test
    void shouldRemoveQueuedUserByEmail() {
        User queuedUser = new User("Fila", "fila@test.com", "pwd", "(11) 99999-9999");
        queuedUser.setId(UUID.randomUUID());
        queuedUser.setStatus(UserStatus.QUEUED);
        queuedUser.setQueuePosition(1);

        given(userRepository.findByEmail("fila@test.com")).willReturn(Optional.of(queuedUser));
        given(userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED)).willReturn(List.of());

        WaitlistMessageDTO response = registrationLimitService.removeQueuedUserByEmail("fila@test.com");

        assertEquals("Voce foi removido da fila de espera.", response.message());
        then(userRepository).should().delete(queuedUser);
        then(waitlistNotificationService).should().notifyRemoved(queuedUser);
    }

    @Test
    void shouldRejectWaitlistRemovalWhenUserIsNotQueued() {
        User activeUser = new User("Ativo", "ativo@test.com", "pwd", "(11) 99999-9999");
        activeUser.setStatus(UserStatus.ACTIVE);

        given(userRepository.findByEmail("ativo@test.com")).willReturn(Optional.of(activeUser));

        assertThrows(
                ResourceNotFoundExceptions.class,
                () -> registrationLimitService.removeQueuedUserByEmail("ativo@test.com")
        );
    }

    @Test
    void shouldPromoteQueuedUsersWhenLimitIsIncreased() {
        AppConfig currentConfig = new AppConfig(RegistrationLimitService.MAX_ACTIVE_USERS_KEY, "2");
        User queuedOne = new User("Fila 1", "fila1@test.com", "pwd", "(11) 99999-0001");
        queuedOne.setStatus(UserStatus.QUEUED);
        queuedOne.setQueuePosition(1);
        User queuedTwo = new User("Fila 2", "fila2@test.com", "pwd", "(11) 99999-0002");
        queuedTwo.setStatus(UserStatus.QUEUED);
        queuedTwo.setQueuePosition(2);

        given(appConfigRepository.findByKeyForUpdate(RegistrationLimitService.MAX_ACTIVE_USERS_KEY))
                .willReturn(Optional.of(currentConfig));
        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(2L, 2L, 4L);
        given(userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED))
                .willReturn(List.of(queuedOne, queuedTwo), List.of());

        MaxUsersConfigResponseDTO response = registrationLimitService.updateMaxActiveUsers(4);

        assertEquals(4, response.maxActiveUsers());
        assertEquals(4L, response.activeUsers());
        assertEquals(2, response.promotedFromQueue());
        assertEquals(UserStatus.ACTIVE, queuedOne.getStatus());
        assertEquals(UserStatus.ACTIVE, queuedTwo.getStatus());
        then(waitlistNotificationService).should().notifyActivated(queuedOne);
        then(waitlistNotificationService).should().notifyActivated(queuedTwo);
    }

    @Test
    void shouldRejectLowerLimitThanCurrentActiveUsers() {
        AppConfig currentConfig = new AppConfig(RegistrationLimitService.MAX_ACTIVE_USERS_KEY, "2");
        given(appConfigRepository.findByKeyForUpdate(RegistrationLimitService.MAX_ACTIVE_USERS_KEY))
                .willReturn(Optional.of(currentConfig));
        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(3L);

        assertThrows(IllegalArgumentException.class, () -> registrationLimitService.updateMaxActiveUsers(2));
    }

    @Test
    void shouldReturnWaitlistSnapshot() {
        User queuedUser = new User("Fila", "fila@test.com", "pwd", "(11) 99999-9999");
        queuedUser.setId(UUID.randomUUID());
        queuedUser.setStatus(UserStatus.QUEUED);
        queuedUser.setQueuePosition(1);

        given(appConfigRepository.findById(RegistrationLimitService.MAX_ACTIVE_USERS_KEY))
                .willReturn(Optional.of(new AppConfig(RegistrationLimitService.MAX_ACTIVE_USERS_KEY, "10")));
        given(userRepository.countByStatus(UserStatus.ACTIVE)).willReturn(8L);
        given(userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED))
                .willReturn(List.of(queuedUser));

        WaitlistResponseDTO response = registrationLimitService.getWaitlist();

        assertEquals(10, response.limit());
        assertEquals(8L, response.activeUsers());
        assertEquals(1, response.queue().size());
        assertEquals(1, response.queue().get(0).position());
        assertEquals("fila@test.com", response.queue().get(0).email());
    }
}
