package com.dvFabricio.VidaLongaFlix.integration.security;

import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// with(csrf()) em outros testes modifica o CsrfFilter via reflexão (WebTestUtils.setCsrfTokenRepository).
// @DirtiesContext garante que este contexto usa o CookieCsrfTokenRepository original.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfProtectionIntegrationTest extends BaseIntegrationTest {

    private static final String LOGIN_BODY = """                                                                                                                                                                                      
          {"email":"admin@vidalongaflix.com","password":"AdminTest@123"}""";

    // GET deve devolver o cookie XSRF-TOKEN legível pelo JavaScript
    @Test
    void shouldSetCsrfCookieOnGetRequest() throws Exception {
        // Spring Security 6.x usa ResponseCookie → addHeader("Set-Cookie"), não addCookie().
        // MockMvc.cookie() só captura cookies via addCookie(), então lemos o header diretamente.
        MvcResult result = mockMvc.perform(get("/auth/registration-status"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
        assertTrue(
                setCookieHeaders.stream().anyMatch(h -> h.startsWith("XSRF-TOKEN=")),
                "Esperado XSRF-TOKEN no header Set-Cookie"
        );
        assertTrue(
                setCookieHeaders.stream()
                        .filter(h -> h.startsWith("XSRF-TOKEN="))
                        .noneMatch(h -> h.toLowerCase().contains("httponly")),
                "XSRF-TOKEN nao deve ser HttpOnly (Angular precisa ler via JavaScript)"
        );
    }

    // GET nunca precisa de token CSRF (não muda estado)
    @Test
    void shouldAllowGetRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk());
    }

    // POST sem nenhum token CSRF deve ser bloqueado
    @Test
    void shouldRejectPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden());
    }

    // POST com cookie e header com valores diferentes deve ser bloqueado
    @Test
    void shouldRejectPostWithMismatchedCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY)
                        .cookie(new Cookie("XSRF-TOKEN", "valor-real"))
                        .header("X-XSRF-TOKEN", "valor-errado"))
                .andExpect(status().isForbidden());
    }

    // Fluxo correto: GET pega o token → POST usa o token → 200
    @Test
    void shouldAllowPostWithValidCsrfToken() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/auth/registration-status"))
                .andExpect(status().isOk())
                .andReturn();

        // Extrai XSRF-TOKEN do header Set-Cookie (Spring Security 6.x usa ResponseCookie)
        List<String> setCookieHeaders = getResult.getResponse().getHeaders("Set-Cookie");
        String csrfToken = setCookieHeaders.stream()
                .filter(h -> h.startsWith("XSRF-TOKEN="))
                .map(h -> h.split(";")[0].substring("XSRF-TOKEN=".length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("XSRF-TOKEN nao encontrado no Set-Cookie header"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY)
                        .cookie(new Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"));
    }
}