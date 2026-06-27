package com.dvFabricio.VidaLongaFlix.services.password;

import com.dvFabricio.VidaLongaFlix.domain.passwordreset.PasswordResetToken;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.TokenExpiredException;
import com.dvFabricio.VidaLongaFlix.repositories.PasswordResetTokenRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetEmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }


    public void requestReset(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            logger.debug("Solicitação de reset para email não cadastrado: {}", email);
            return;
        }

        User user = optionalUser.get();

        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.debug("Solicitação de reset para usuário não-ACTIVE: {}", email);
            return;
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String tokenValue = HexFormat.of().formatHex(bytes);

        try {
            emailService.sendResetEmail(user.getName(), user.getEmail(), tokenValue);
        } catch (Exception e) {
            logger.error("Falha ao enviar email de reset para {}. Token não salvo.", email, e);
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        PasswordResetToken token = new PasswordResetToken(
                user,
                tokenValue,
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );
        tokenRepository.save(token);
    }

    public void validateToken(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Token não encontrado."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expirado. Solicite um novo link.");
        }

        if (token.isUsed()) {
            throw new TokenExpiredException("Token já utilizado. Solicite um novo link.");
        }
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Token não encontrado."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expirado. Solicite um novo link.");
        }

        if (token.isUsed()) {
            throw new TokenExpiredException("Token já utilizado. Solicite um novo link.");
        }

        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        // try-catch aqui porque no teste o mock de PasswordResetEmailService substitui
        // a classe inteira — o try-catch interno de sendChangeConfirmation não executa.
        // Em produção o try-catch do PasswordResetEmailService já bastaria,
        // mas para os testes funcionarem corretamente precisamos do catch aqui também.
        try {
            emailService.sendChangeConfirmation(user.getName(), user.getEmail());
        } catch (Exception e) {
            logger.error("Falha ao enviar confirmação de reset para {}: {}", user.getEmail(), e.getMessage());
        }
    }
}