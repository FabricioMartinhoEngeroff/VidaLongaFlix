package com.dvFabricio.VidaLongaFlix.controllers.auth;

import com.dvFabricio.VidaLongaFlix.domain.passwordreset.PasswordResetRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.passwordreset.ResetPasswordRequestDTO;
import com.dvFabricio.VidaLongaFlix.services.password.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/password-recovery")
    public ResponseEntity<Map<String, String>> requestReset(
            @RequestBody @Valid PasswordResetRequestDTO body) {

        passwordResetService.requestReset(body.email());

        return ResponseEntity.ok(Map.of(
                "message", "Se o seu email estiver cadastrado, você receberá um link em breve."
        ));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        passwordResetService.validateToken(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Valid ResetPasswordRequestDTO body) {
        passwordResetService.resetPassword(body.token(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
