package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream().map(UserDTO::new).toList();
    }

    public UserDTO findUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
    }

    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        validateUserRequestDTO(userRequestDTO);
        User user = new User(
                userRequestDTO.login(),
                userRequestDTO.email(),
                userRequestDTO.password()
        );
        userRepository.save(user);
        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(UUID userId, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
        if (userRequestDTO.login() != null && !userRequestDTO.login().isBlank()) {
            user.setLogin(userRequestDTO.login());
        }
        if (userRequestDTO.email() != null && !userRequestDTO.email().isBlank()) {
            user.setEmail(userRequestDTO.email());
        }
        if (userRequestDTO.password() != null && !userRequestDTO.password().isBlank()) {
            user.setPassword(userRequestDTO.password());
        }
        userRepository.save(user);
        return new UserDTO(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
        userRepository.delete(user);
    }

    private void validateUserRequestDTO(UserRequestDTO userRequestDTO) {
        if (userRequestDTO.login() == null || userRequestDTO.login().isBlank()) {
            throw new MissingRequiredFieldException("login", "Login cannot be empty.");
        }
        if (userRequestDTO.email() == null || userRequestDTO.email().isBlank()) {
            throw new MissingRequiredFieldException("email", "Email cannot be empty.");
        }
        if (userRequestDTO.password() == null || userRequestDTO.password().isBlank()) {
            throw new MissingRequiredFieldException("password", "Password cannot be empty.");
        }
    }
}
