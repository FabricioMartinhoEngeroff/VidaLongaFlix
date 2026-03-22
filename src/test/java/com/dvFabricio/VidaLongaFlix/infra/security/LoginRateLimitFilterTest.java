package com.dvFabricio.VidaLongaFlix.infra.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do filtro de rate limiting no login.
 *
 * Cada teste cria uma nova instância do filtro para garantir
 * que os baldes de tokens estejam zerados (sem estado compartilhado).
 */
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    // ─── Cenário 1: requisições dentro do limite ──────────────────────────────

    @Test
    void shouldAllowRequestsWithinLimit() throws ServletException, IOException {
        // O balde tem 5 fichas. 5 tentativas devem passar sem bloqueio.
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = doLoginRequest("192.168.1.1");
            assertThat(response.getStatus())
                    .as("Tentativa %d deve ser permitida (status != 429)", i + 1)
                    .isNotEqualTo(429);
        }
    }

    // ─── Cenário 2: bloqueio na 6ª tentativa ─────────────────────────────────

    @Test
    void shouldBlockAfterFiveAttempts() throws ServletException, IOException {
        // Esgota as 5 fichas
        for (int i = 0; i < 5; i++) {
            doLoginRequest("192.168.1.10");
        }

        // A 6ª tentativa deve ser bloqueada
        MockHttpServletResponse response = doLoginRequest("192.168.1.10");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString())
                .contains("RATE_LIMIT_EXCEEDED");
    }

    // ─── Cenário 3: IPs diferentes têm baldes independentes ──────────────────

    @Test
    void shouldTrackBucketsSeparatelyPerIp() throws ServletException, IOException {
        // IP A esgota o balde
        for (int i = 0; i < 5; i++) {
            doLoginRequest("10.0.0.1");
        }

        // IP B não deve ser afetado pelo IP A
        MockHttpServletResponse responseIpB = doLoginRequest("10.0.0.2");
        assertThat(responseIpB.getStatus())
                .as("IP diferente deve ter balde independente")
                .isNotEqualTo(429);
    }

    // ─── Cenário 4: endpoints que não são login não são bloqueados ────────────

    @Test
    void shouldNotApplyRateLimitToOtherEndpoints() throws ServletException, IOException {
        // Simula um IP já bloqueado no login
        for (int i = 0; i < 6; i++) {
            doLoginRequest("172.16.0.5");
        }

        // A mesma origem fazendo GET em outro endpoint não deve ser bloqueada
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos");
        request.setRemoteAddr("172.16.0.5");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus())
                .as("Outros endpoints não devem ser bloqueados pelo rate limit do login")
                .isNotEqualTo(429);
    }

    // ─── Cenário 5: X-Forwarded-For (proxy / CloudFront / ALB) ───────────────

    @Test
    void shouldUseForwardedIpWhenBehindProxy() throws ServletException, IOException {
        String realUserIp = "203.0.113.42";

        // Esgota o balde usando X-Forwarded-For
        for (int i = 0; i < 5; i++) {
            doLoginRequestWithForwardedIp("10.0.0.100", realUserIp);
        }

        // 6ª tentativa do mesmo IP real deve ser bloqueada
        MockHttpServletResponse response = doLoginRequestWithForwardedIp("10.0.0.100", realUserIp);
        assertThat(response.getStatus())
                .as("Rate limit deve usar o IP real do X-Forwarded-For, não o IP do proxy")
                .isEqualTo(429);
    }

    // ─── Cenário 6: resolução correta do IP com múltiplos proxies ────────────

    @Test
    void shouldExtractFirstIpFromForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Cadeia de proxies: o primeiro IP é o do usuário real
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 10.0.0.2");

        String ip = filter.resolveClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.1");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MockHttpServletResponse doLoginRequest(String remoteAddr)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse doLoginRequestWithForwardedIp(String remoteAddr,
                                                                   String forwardedFor)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}