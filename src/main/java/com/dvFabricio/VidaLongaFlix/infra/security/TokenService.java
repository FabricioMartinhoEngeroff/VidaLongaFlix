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
    private String fixedUserEmail;

    public String generateToken(User user) {
        if (useFixedToken) {
            return fixedToken;
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("VidaLongaFlix")
                    .withSubject(user.getEmail())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new IllegalStateException("Failed to generate token", e);
        }
    }

    public String validateToken(String token) {
        if (useFixedToken && fixedToken.equals(token)) {
            return fixedUserEmail;
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

    private Instant generateExpirationDate() {
        return LocalDateTime.now()
                .plusHours(expirationHours)
                .toInstant(ZoneOffset.UTC);
    }
}