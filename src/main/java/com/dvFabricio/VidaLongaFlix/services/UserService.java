package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WelcomeService welcomeService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, WelcomeService welcomeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.welcomeService = welcomeService;
    }

    public UserDTO findAuthenticatedUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Usuário não encontrado com ID: " + userId));
    }

    public UserDTO findUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
    }

    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        validateRequiredFields(userRequestDTO);

        if (userRepository.existsByEmail(userRequestDTO.email())) {
            throw new DuplicateResourceException("email", "A user with this email already exists.");
        }

        if (userRepository.existsByTaxId(userRequestDTO.taxId())) {
            throw new DuplicateResourceException("taxId", "A user with this CPF already exists.");
        }

        String encodedPassword = passwordEncoder.encode(userRequestDTO.password());

        User user = new User(
                userRequestDTO.name(),
                userRequestDTO.email(),
                encodedPassword,
                userRequestDTO.taxId(),
                userRequestDTO.phone(),
                userRequestDTO.address()
        );

        Role defaultRole = new Role("ROLE_USER");
        user.getRoles().add(defaultRole);

        user = userRepository.save(user);

        welcomeService.sendWelcomeMessage(user.getName(), user.getPhone());

        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(UUID userId, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));

        logger.debug("Before update: {}", user);
        updateUserFields(user, userRequestDTO);
        user = userRepository.save(user);
        logger.debug("After update: {}", user);

        return new UserDTO(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
        userRepository.delete(user);
    }

    private void updateUserFields(User user, UserRequestDTO dto) {
        Optional.ofNullable(dto.name()).ifPresent(user::setName);
        Optional.ofNullable(dto.email()).ifPresent(user::setEmail);
        Optional.ofNullable(dto.password()).filter(p -> !p.isBlank())
                .ifPresent(p -> user.setPassword(passwordEncoder.encode(p)));
        Optional.ofNullable(dto.taxId()).ifPresent(user::setTaxId);
        Optional.ofNullable(dto.phone()).ifPresent(user::setPhone);
        Optional.ofNullable(dto.address()).ifPresent(user::setAddress);
    }

    private void validateRequiredFields(UserRequestDTO dto) {
        validateField("name", dto.name());
        validateField("email", dto.email());
        validateField("password", dto.password());
        validateField("taxId", dto.taxId());
        validateField("phone", dto.phone());

        if (dto.address() == null) {
            throw new MissingRequiredFieldException("address", "Address cannot be empty");
        }
    }

    private void validateField(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(fieldName, fieldName + " cannot be empty");
        }
    }
}
