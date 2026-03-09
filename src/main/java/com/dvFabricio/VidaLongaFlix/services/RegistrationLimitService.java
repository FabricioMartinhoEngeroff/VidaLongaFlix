package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.auth.QueueLoginErrorDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationStatusDTO;
import com.dvFabricio.VidaLongaFlix.domain.config.AppConfig;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.MaxUsersConfigResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistEntryDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistMessageDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistResponseDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.AppConfigRepository;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistrationLimitService {

    public static final String MAX_ACTIVE_USERS_KEY = "MAX_ACTIVE_USERS";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AppConfigRepository appConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final WelcomeService welcomeService;
    private final WaitlistNotificationService waitlistNotificationService;

    public RegistrationLimitService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AppConfigRepository appConfigRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            WelcomeService welcomeService,
            WaitlistNotificationService waitlistNotificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.appConfigRepository = appConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.welcomeService = welcomeService;
        this.waitlistNotificationService = waitlistNotificationService;
    }

    @Transactional
    public RegistrationResponseDTO register(RegisterRequestDTO body) {
        Optional<User> existingUser = userRepository.findByEmail(body.email());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getStatus() == UserStatus.QUEUED) {
                throw new DuplicateResourceException("email",
                        "You are already on the waitlist. Current position: #" + user.getQueuePosition() + ".");
            }
            throw new DuplicateResourceException("email", "Email is already in use.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundExceptions("Role 'ROLE_USER' not found"));

        int maxActiveUsers = getMaxActiveUsersForUpdate();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);

        User newUser = new User(
                body.name(),
                body.email(),
                passwordEncoder.encode(body.password()),
                body.phone()
        );
        newUser.setRoles(List.of(userRole));

        if (activeUsers < maxActiveUsers) {
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setQueuePosition(null);
            userRepository.save(newUser);
            sendWelcomeBestEffort(newUser);
            return new RegistrationResponseDTO(
                    generateTokenFor(newUser),
                    new UserResponseDTO(newUser),
                    false,
                    null,
                    null
            );
        }

        int nextPosition = userRepository.findMaxQueuePosition(UserStatus.QUEUED).orElse(0) + 1;
        newUser.setStatus(UserStatus.QUEUED);
        newUser.setQueuePosition(nextPosition);
        userRepository.save(newUser);
        notifyQueuedBestEffort(newUser);

        return new RegistrationResponseDTO(
                null,
                new UserResponseDTO(newUser),
                true,
                nextPosition,
                "Limite de usuarios atingido. Voce foi adicionado a fila de espera na posicao #" + nextPosition + "."
        );
    }

    public RegistrationStatusDTO getRegistrationStatus() {
        int limit = getMaxActiveUsers();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long queueSize = userRepository.countByStatus(UserStatus.QUEUED);
        return new RegistrationStatusDTO(activeUsers < limit, activeUsers, limit, queueSize);
    }

    public QueueLoginErrorDTO buildQueuedLoginError(User user) {
        return new QueueLoginErrorDTO(
                "ACCOUNT_QUEUED",
                "Sua conta esta na fila de espera. Voce sera notificado quando sua vaga for liberada.",
                user.getQueuePosition()
        );
    }

    public QueueLoginErrorDTO buildDisabledLoginError() {
        return new QueueLoginErrorDTO(
                "ACCOUNT_DISABLED",
                "Sua conta foi desativada.",
                null
        );
    }

    public WaitlistResponseDTO getWaitlist() {
        return new WaitlistResponseDTO(
                getMaxActiveUsers(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED)
                        .stream()
                        .map(WaitlistEntryDTO::new)
                        .toList()
        );
    }

    @Transactional
    public RegistrationResponseDTO activateQueuedUser(UUID userId) {
        int limit = getMaxActiveUsersForUpdate();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        if (activeUsers >= limit) {
            throw new IllegalArgumentException("No available slots to activate a queued user.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.QUEUED) {
            throw new IllegalArgumentException("User is not in the waitlist.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setQueuePosition(null);
        userRepository.save(user);
        recalculateQueuePositions();
        notifyActivatedBestEffort(user);

        return new RegistrationResponseDTO(
                null,
                new UserResponseDTO(user),
                false,
                null,
                "Usuario ativado com sucesso."
        );
    }

    @Transactional
    public WaitlistMessageDTO removeQueuedUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.QUEUED) {
            throw new IllegalArgumentException("User is not in the waitlist.");
        }

        userRepository.delete(user);
        recalculateQueuePositions();
        notifyRemovedBestEffort(user);
        return new WaitlistMessageDTO("Usuario removido da fila.");
    }

    @Transactional
    public WaitlistMessageDTO removeQueuedUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Voce nao esta na fila de espera."));

        if (user.getStatus() != UserStatus.QUEUED) {
            throw new ResourceNotFoundExceptions("Voce nao esta na fila de espera.");
        }

        userRepository.delete(user);
        recalculateQueuePositions();
        notifyRemovedBestEffort(user);
        return new WaitlistMessageDTO("Voce foi removido da fila de espera.");
    }

    @Transactional
    public MaxUsersConfigResponseDTO updateMaxActiveUsers(int maxActiveUsers) {
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        if (maxActiveUsers < activeUsers) {
            throw new IllegalArgumentException(
                    "Limite nao pode ser menor que o total de usuarios ativos (" + activeUsers + ")."
            );
        }

        AppConfig config = getConfigForUpdate();
        config.setValue(String.valueOf(maxActiveUsers));
        appConfigRepository.save(config);

        int promoted = promoteQueuedUsersToAvailableSlots(maxActiveUsers);
        return new MaxUsersConfigResponseDTO(
                maxActiveUsers,
                userRepository.countByStatus(UserStatus.ACTIVE),
                promoted
        );
    }

    @Transactional
    public int promoteQueuedUsersToAvailableSlots() {
        return promoteQueuedUsersToAvailableSlots(getMaxActiveUsersForUpdate());
    }

    private int promoteQueuedUsersToAvailableSlots(int maxActiveUsers) {
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        if (activeUsers >= maxActiveUsers) {
            return 0;
        }

        int promoted = 0;
        List<User> queuedUsers = userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED);
        for (User queuedUser : queuedUsers) {
            if (activeUsers >= maxActiveUsers) {
                break;
            }
            queuedUser.setStatus(UserStatus.ACTIVE);
            queuedUser.setQueuePosition(null);
            userRepository.save(queuedUser);
            activeUsers++;
            promoted++;
            notifyActivatedBestEffort(queuedUser);
        }

        recalculateQueuePositions();
        return promoted;
    }

    private void recalculateQueuePositions() {
        List<User> queuedUsers = userRepository.findByStatusOrderByQueuePositionAsc(UserStatus.QUEUED);
        for (int i = 0; i < queuedUsers.size(); i++) {
            queuedUsers.get(i).setQueuePosition(i + 1);
        }
        userRepository.saveAll(queuedUsers);
    }

    private AppConfig getConfigForUpdate() {
        return appConfigRepository.findByKeyForUpdate(MAX_ACTIVE_USERS_KEY)
                .orElseGet(() -> appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "100")));
    }

    private int getMaxActiveUsersForUpdate() {
        return Integer.parseInt(getConfigForUpdate().getValue());
    }

    public int getMaxActiveUsers() {
        return Integer.parseInt(appConfigRepository.findById(MAX_ACTIVE_USERS_KEY)
                .orElse(new AppConfig(MAX_ACTIVE_USERS_KEY, "100"))
                .getValue());
    }

    private void sendWelcomeBestEffort(User user) {
        try {
            welcomeService.sendWelcomeMessage(user.getName(), user.getPhone());
        } catch (Exception e) {
            System.err.println("WhatsApp nao enviado: " + e.getMessage());
        }
    }

    private void notifyQueuedBestEffort(User user) {
        try {
            waitlistNotificationService.notifyQueued(user);
        } catch (Exception e) {
            System.err.println("Fila nao notificada: " + e.getMessage());
        }
    }

    private void notifyActivatedBestEffort(User user) {
        try {
            waitlistNotificationService.notifyActivated(user);
        } catch (Exception e) {
            System.err.println("Ativacao nao notificada: " + e.getMessage());
        }
    }

    private void notifyRemovedBestEffort(User user) {
        try {
            waitlistNotificationService.notifyRemoved(user);
        } catch (Exception e) {
            System.err.println("Remocao da fila nao notificada: " + e.getMessage());
        }
    }

    private String generateTokenFor(User user) {
        return tokenService.generateToken(user);
    }
}
