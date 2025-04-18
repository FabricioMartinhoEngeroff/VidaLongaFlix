package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.*;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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


    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        UUID userId = tokenService.getUserIdFromToken(token);
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Usuário não encontrado"));

        return ResponseEntity.ok(new UserProfileDTO(
                user.getName(), user.getEmail(), user.getCpf(), user.getTelefone(), user.getEndereco()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO body) {
        if (body.email() == null || body.email().isBlank()) {
            return ResponseEntity.badRequest().body("Email cannot be empty.");
        }

        try {
            User user = repository.findByEmail(body.email())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!passwordEncoder.matches(body.password(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            String token = tokenService.generateToken(user);
            return ResponseEntity.ok(new TokenDTO(token));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDTO body) {
        if (repository.existsByEmail(body.email())) {
            return ResponseEntity.badRequest().body("Email is already in use.");
        }

        try {
            User newUser = new User(
                    body.name(),
                    body.email(),
                    passwordEncoder.encode(body.password()),
                    body.cpf(),
                    body.telefone(),
                    body.endereco()
            );

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ResourceNotFoundExceptions("Role 'ROLE_USER' not found"));

            newUser.setRoles(List.of(userRole));
            repository.save(newUser);

            String token = tokenService.generateToken(newUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(new TokenDTO(token));

        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
