package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.MaxUsersConfigRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.MaxUsersConfigResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistMessageDTO;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistResponseDTO;
import com.dvFabricio.VidaLongaFlix.services.RegistrationLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWaitlistController {

    private final RegistrationLimitService registrationLimitService;

    @GetMapping("/waitlist")
    public ResponseEntity<WaitlistResponseDTO> getWaitlist() {
        return ResponseEntity.ok(registrationLimitService.getWaitlist());
    }

    @PostMapping("/waitlist/{userId}/activate")
    public ResponseEntity<RegistrationResponseDTO> activateWaitlistedUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(registrationLimitService.activateQueuedUser(userId));
    }

    @DeleteMapping("/waitlist/{userId}")
    public ResponseEntity<WaitlistMessageDTO> removeWaitlistedUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(registrationLimitService.removeQueuedUser(userId));
    }

    @PutMapping("/config/max-users")
    public ResponseEntity<MaxUsersConfigResponseDTO> updateMaxActiveUsers(
            @RequestBody @Valid MaxUsersConfigRequestDTO request
    ) {
        return ResponseEntity.ok(registrationLimitService.updateMaxActiveUsers(request.maxActiveUsers()));
    }
}
