package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe base para testes de integração.
 * <p>
 * Sobe o contexto Spring completo (H2 em memória + segurança JWT).
 * O DataInitializer cria ROLE_USER, ROLE_ADMIN e o admin padrão no startup.
 * O WhatsApp fica desabilitado para não fazer chamadas reais.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "whatsapp.enabled=false",
                // Credenciais de admin usadas apenas nos testes de integração
                "admin.email=admin@vidalongaflix.com",
                "admin.password=AdminTest@123"
        }
)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@vidalongaflix.com";
    protected static final String ADMIN_PASSWORD = "AdminTest@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Faz login com o admin padrão (criado pelo DataInitializer) e retorna o token JWT.
     */
    protected String getAdminToken() throws Exception {
        return loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /**
     * Faz login via endpoint real e retorna o Bearer token.
     */
    protected String loginAs(String email, String password) throws Exception {
        LoginRequestDTO body = new LoginRequestDTO(email, password);
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    /**
     * Adiciona o header Authorization: Bearer {token} na requisição.
     */
    protected MockHttpServletRequestBuilder bearer(
            MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
