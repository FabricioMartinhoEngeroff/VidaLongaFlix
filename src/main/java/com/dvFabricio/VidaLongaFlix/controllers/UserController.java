package com.dvFabricio.VidaLongaFlix.controllers;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> findAuthenticatedUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UserDTO(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findUserById(@PathVariable UUID id) {
        UserDTO userDTO = userService.findUserById(id);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid UserRequestDTO userRequestDTO) {
        UserDTO created = userService.createUser(userRequestDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody @Valid UserRequestDTO userRequestDTO) {
        UserDTO updated = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
