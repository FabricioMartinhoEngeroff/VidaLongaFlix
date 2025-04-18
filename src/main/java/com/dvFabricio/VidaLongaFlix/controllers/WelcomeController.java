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
    public ResponseEntity<String> registrarUsuario(
            @RequestParam @NotBlank String nome,
            @RequestParam @NotBlank String telefone) {

        welcomeService.enviarBoasVindas(nome, telefone);
        return ResponseEntity.ok("Mensagem de boas-vindas enviada para " + nome);
    }
}
