package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.message.Message;
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

        if (userRepository.existsByCpf(userRequestDTO.cpf())) {
            throw new DuplicateResourceException("cpf", "A user with this CPF already exists.");
        }

        String encodedPassword = passwordEncoder.encode(userRequestDTO.password());

        User user = new User(
                userRequestDTO.name(),
                userRequestDTO.email(),
                encodedPassword,
                userRequestDTO.cpf(),
                userRequestDTO.telefone(),
                userRequestDTO.endereco()
        );

        Role defaultRole = new Role("ROLE_USER");
        user.getRoles().add(defaultRole);

        user = userRepository.save(user);

        welcomeService.enviarBoasVindas(user.getName(), user.getTelefone());

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
        Optional.ofNullable(dto.cpf()).ifPresent(user::setCpf);
        Optional.ofNullable(dto.telefone()).ifPresent(user::setTelefone);
        Optional.ofNullable(dto.endereco()).ifPresent(user::setEndereco);
    }

    private void validateRequiredFields(UserRequestDTO dto) {
        validateField("name", dto.name());
        validateField("email", dto.email());
        validateField("password", dto.password());
        validateField("cpf", dto.cpf());
        validateField("telefone", dto.telefone());

        if (dto.endereco() == null) {
            throw new MissingRequiredFieldException("endereco", "Address cannot be empty");
        }
    }

    private void validateField(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(fieldName, fieldName + " cannot be empty");
        }
    }
}

