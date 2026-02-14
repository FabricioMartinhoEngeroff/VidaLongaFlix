package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/onboarding")
@RequiredArgsConstructor
public class WelcomeController {

    private final WelcomeService welcomeService;

    @PostMapping
    public ResponseEntity<String> registerUser(
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String phone) {

        welcomeService.sendWelcomeMessage(name, phone);
        return ResponseEntity.ok("Mensagem de boas-vindas enviada para " + name);
    }
}
