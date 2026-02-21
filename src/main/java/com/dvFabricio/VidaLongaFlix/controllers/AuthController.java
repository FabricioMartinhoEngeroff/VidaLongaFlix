package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.AuthResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.InvalidCredentialsException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final WelcomeService welcomeService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(
            @RequestHeader("Authorization") String bearerToken) {

        String token = bearerToken.replace("Bearer ", "");
        UUID userId = tokenService.getUserIdFromToken(token);

        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("User not found"));

        return ResponseEntity.ok(new UserResponseDTO(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO body) {

        User user = repository.findByEmail(body.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(body.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = tokenService.generateToken(user);
        return ResponseEntity.ok(new AuthResponseDTO(token, new UserResponseDTO(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @RequestBody @Valid RegisterRequestDTO body) {

        if (repository.existsByEmail(body.email())) {
            throw new DuplicateResourceException("email", "Email is already in use.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundExceptions("Role 'ROLE_USER' not found"));

        User newUser = new User(
                body.name(),
                body.email(),
                passwordEncoder.encode(body.password()),
                body.phone()
        );
        newUser.setRoles(List.of(userRole));
        repository.save(newUser);

        try {
            welcomeService.sendWelcomeMessage(newUser.getName(), newUser.getPhone());
        } catch (Exception e) {
            System.out.println("WhatsApp não enviado: " + e.getMessage());
        }

        String token = tokenService.generateToken(newUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDTO(token, new UserResponseDTO(newUser)));
    }
}
