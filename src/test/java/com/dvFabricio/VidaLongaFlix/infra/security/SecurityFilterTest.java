package com.dvFabricio.VidaLongaFlix.infra.security;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @InjectMocks
    private SecurityFilter securityFilter;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserWhenBearerTokenIsValid() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        User user = new User("João Silva", "joao@example.com", "encodedPassword", "(11) 99999-9999");
        user.setId(userId);
        user.setRoles(List.of(new Role("ROLE_USER")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token.valido.aqui");

        when(tokenService.getUserIdFromToken("token.valido.aqui")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertSame(user, principal);
    }

    @Test
    void shouldIgnoreRequestWhenAuthorizationHeaderIsNotBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");

        securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).getUserIdFromToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldLeaveRequestUnauthenticatedWhenTokenIsInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token.invalido.aqui");

        when(tokenService.getUserIdFromToken("token.invalido.aqui"))
                .thenThrow(new RuntimeException("Token inválido"));

        securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldLeaveRequestUnauthenticatedWhenUserDoesNotExist() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token.valido.aqui");

        when(tokenService.getUserIdFromToken("token.valido.aqui")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
