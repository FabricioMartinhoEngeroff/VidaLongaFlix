package com.dvFabricio.VidaLongaFlix.controllers.user;
import com.dvFabricio.VidaLongaFlix.domain.user.UserDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.UserRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.services.auth.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('ADMIN') or #id.toString().equals(authentication.principal.id.toString())")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findUserById(@PathVariable UUID id) {
        UserDTO userDTO = userService.findUserById(id);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid UserRequestDTO userRequestDTO) {
        UserDTO created = userService.createUser(userRequestDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasRole('ADMIN') or #id.toString().equals(authentication.principal.id.toString())")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody @Valid UserRequestDTO userRequestDTO) {
        UserDTO updated = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
