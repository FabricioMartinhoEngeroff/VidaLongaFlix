package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.auth.AuthResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.QueueLoginErrorDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationStatusDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.UserResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistMessageDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.InvalidCredentialsException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.RegistrationLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RegistrationLimitService registrationLimitService;

    // @AuthenticationPrincipal injeta o usuário já autenticado pelo SecurityFilter
    // Não precisa mais extrair token manualmente — o Spring já fez isso
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new ResourceNotFoundExceptions("User not authenticated");
        }

        return ResponseEntity.ok(new UserResponseDTO(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequestDTO body) {

        User user = repository.findByEmail(body.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getStatus() == UserStatus.QUEUED) {
            QueueLoginErrorDTO response = registrationLimitService.buildQueuedLoginError(user);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            QueueLoginErrorDTO response = registrationLimitService.buildDisabledLoginError();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (!passwordEncoder.matches(body.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = tokenService.generateToken(user);
        return ResponseEntity.ok(new AuthResponseDTO(token, new UserResponseDTO(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(
            @RequestBody @Valid RegisterRequestDTO body) {
        RegistrationResponseDTO response = registrationLimitService.register(body);
        HttpStatus status = response.queued() ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/registration-status")
    public ResponseEntity<RegistrationStatusDTO> registrationStatus() {
        return ResponseEntity.ok(registrationLimitService.getRegistrationStatus());
    }

    @DeleteMapping("/waitlist/me")
    public ResponseEntity<WaitlistMessageDTO> cancelWaitlistEntry(
            @RequestParam String email) {
        return ResponseEntity.ok(registrationLimitService.removeQueuedUserByEmail(email));
    }
}
