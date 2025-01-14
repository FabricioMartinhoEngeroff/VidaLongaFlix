package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO getUserDTOById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MissingRequiredFieldException("user", "Usuário não encontrado."));
        validateUser(user);
        return new UserDTO(user);
    }


    private void validateUser(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new MissingRequiredFieldException("login", "O campo login não pode estar vazio.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new MissingRequiredFieldException("email", "O campo email não pode estar vazio.");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new MissingRequiredFieldException("password", "O campo senha não pode estar vazio.");
        }
    }
}

