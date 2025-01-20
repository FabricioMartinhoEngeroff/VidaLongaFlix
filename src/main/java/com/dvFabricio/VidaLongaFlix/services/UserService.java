package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.DuplicateResourceException;
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
        return userRepository.findAll()
                .stream()
                .map(UserDTO::new)
                .toList();
    }


    public UserDTO findUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
    }


    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        if (isBlank(userRequestDTO.login())) {
            throw new MissingRequiredFieldException("login", "Login não pode estar vazio");
        }
        if (isBlank(userRequestDTO.email())) {
            throw new MissingRequiredFieldException("email", "Email não pode estar vazio");
        }
        if (isBlank(userRequestDTO.password())) {
            throw new MissingRequiredFieldException("password", "Senha não pode estar vazia");
        }

        if (userRepository.findByEmail(userRequestDTO.email()).isPresent()) {
            throw new DuplicateResourceException("email", "Email is already in use.");
        }

        User user = new User(userRequestDTO.login(), userRequestDTO.email(), userRequestDTO.password());

        user = userRepository.save(user);

        return new UserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(UUID userId, UserRequestDTO userRequestDTO) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));

        System.out.println("Before update: " + user);
        updateUserFields(user, userRequestDTO);
        user = userRepository.save(user);
        System.out.println("After update: " + user);

        return new UserDTO(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found with id: " + userId));
        userRepository.delete(user);
    }

    private void updateUserFields(User user, UserRequestDTO userRequestDTO) {
        if (!isBlank(userRequestDTO.login())) {
            user.setLogin(userRequestDTO.login());
        }
        if (!isBlank(userRequestDTO.email())) {
            user.setEmail(userRequestDTO.email());
        }
        if (!isBlank(userRequestDTO.password())) {
            user.setPassword(userRequestDTO.password());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

