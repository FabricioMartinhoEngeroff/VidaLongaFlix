package com.dvFabricio.VidaLongaFlix.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private int expirationHours;

    @Value("${api.security.token.fixed:false}")
    private boolean useFixedToken;

    @Value("${api.security.token.fixed.value:}")
    private String fixedToken;

    @Value("${api.security.user.fixed.id:}")
    private String fixedUserId;

    public String generateToken(User user) {
        if (useFixedToken) {
            return fixedToken;
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("VidaLongaFlix")
                    .withSubject(user.getId().toString())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new IllegalStateException("Failed to generate token", e);
        }
    }

    public String validateToken(String token) {
        if (useFixedToken && fixedToken.equals(token)) {
            return fixedUserId;
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("VidaLongaFlix")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (TokenExpiredException exception) {
            throw new JwtException("Token JWT expirado. Faça login novamente.", exception);
        } catch (JWTVerificationException exception) {
            throw new JwtException("Token JWT inválido.", exception);
        }
    }

    public UUID getUserIdFromToken(String token) {
        String userIdString = validateToken(token);
        try {
            return UUID.fromString(userIdString);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Token contém um ID inválido.", e);
        }
    }

    private Instant generateExpirationDate() {
        return LocalDateTime.now()
                .plusHours(expirationHours)
                .toInstant(ZoneOffset.UTC);
    }
}