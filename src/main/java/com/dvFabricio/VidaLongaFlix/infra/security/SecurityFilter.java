package com.dvFabricio.VidaLongaFlix.infra.security;

import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.AccessDeniedException;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = recoverToken(request);

        if (token != null) {
            try {
                // Validação do token
                String email = tokenService.validateToken(token);
                if (email != null) {
                    // Recupera o usuário associado ao token
                    userRepository.findByEmail(email).ifPresent(user -> {
                        // Comentado a verificação de permissões, pois você deseja deixar o acesso livre
                        // if (!user.hasPermissionToAccess("ADMIN")) {
                        //     throw new AccessDeniedException("Acesso negado para o usuário " + email);
                        // }
                        // Se o usuário for validado, o sistema deve permitir o acesso independentemente de suas roles
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
                }
            } catch (JwtException e) {
                // Se houver um erro relacionado ao token, retorna um status 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token inválido: " + e.getMessage());
                return;
            } catch (AccessDeniedException e) {
                // Se o usuário não tiver permissão, retorna um status 403 (Forbidden)
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Acesso negado: " + e.getMessage());
                return;
            }
        }

        // Continuação do filtro, se o token for válido e o usuário autorizado
        filterChain.doFilter(request, response);
    }


    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
    }
}
