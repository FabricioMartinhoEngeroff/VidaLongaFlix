package com.dvFabricio.VidaLongaFlix.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para entradas inválidas:
 * <p>
 * - UUID malformado em path variables → 400 (não 500)
 * - JSON malformado no body → 400
 * - Enum inválido em query param → 400
 * - Body vazio quando esperado JSON → 400
 */
class InvalidInputIntegrationTest extends BaseIntegrationTest {

    // ─────────────────── UUID INVÁLIDO NOS PATH VARIABLES ─────────────────

    @Test
    void shouldReturn400ForInvalidUuidInVideoPath() throws Exception {
        mockMvc.perform(get("/videos/{id}", "isso-nao-e-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForInvalidUuidInMenuPath() throws Exception {
        mockMvc.perform(get("/menus/{id}", "nao-e-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForInvalidUuidInCommentsByVideoPath() throws Exception {
        mockMvc.perform(get("/comments/video/{id}", "uuid-invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForInvalidUuidInCategoryDeletePath() throws Exception {
        String adminToken = getAdminToken();

        mockMvc.perform(delete("/categories/{id}", "uuid-invalido"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────── ENUM INVÁLIDO EM QUERY PARAM ─────────────────────

    @Test
    void shouldReturn400ForInvalidCategoryTypeEnum() throws Exception {
        mockMvc.perform(get("/categories").param("type", "TIPO_INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────── JSON MALFORMADO / BODY INVÁLIDO ──────────────────

    @Test
    void shouldReturn400ForMalformedJsonOnLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json valido}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMalformedJsonOnCategoryCreate() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{chave sem aspas: valor}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMissingCategoryTypeParam() throws Exception {
        // GET /categories sem o param obrigatório ?type=
        mockMvc.perform(get("/categories"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────── UUID VÁLIDO MAS INEXISTENTE ──────────────────────
    // Garante que UUID bem formado mas sem registro retorna 404, não 500

    @Test
    void shouldReturn404ForWellFormedButNonExistentVideoUuid() throws Exception {
        mockMvc.perform(get("/videos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForWellFormedButNonExistentMenuUuid() throws Exception {
        mockMvc.perform(get("/menus/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
