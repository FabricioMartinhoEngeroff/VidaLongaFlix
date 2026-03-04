package com.dvFabricio.VidaLongaFlix.integration.security;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração focados nas regras de controle de acesso (segurança):
 * <p>
 * - Endpoints públicos devem retornar 200 sem token
 * - Endpoints admin devem retornar 403 sem token ou com ROLE_USER
 * - Endpoints admin devem funcionar com ROLE_ADMIN
 * - Endpoints autenticados devem retornar 403 sem token
 */
class SecurityAccessIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private UUID testCategoryId;

    private static final String TEST_USER_EMAIL = "sec-test-user@integration.test";

    @BeforeEach
    void setUp() throws Exception {
        // Cria usuário com apenas ROLE_USER para testar restrições de acesso
        if (!userRepository.existsByEmail(TEST_USER_EMAIL)) {
            Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
            User regularUser = new User(
                    "Security Test User", TEST_USER_EMAIL,
                    passwordEncoder.encode("SecTest@1234"), "(51) 99999-0001");
            regularUser.setTaxId("111.111.111-" + UUID.randomUUID().toString().substring(0, 2));
            regularUser.setRoles(List.of(userRole));
            userRepository.save(regularUser);
        }

        // Cria categoria para testar criação de vídeo como admin
        Category category = categoryRepository.save(
                new Category("Sec-Test-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        testCategoryId = category.getId();

        adminToken = getAdminToken();
        userToken = loginAs(TEST_USER_EMAIL, "SecTest@1234");
    }

    @AfterEach
    void cleanup() {
        // Remove vídeos da categoria antes (FK) — inclui o criado em shouldAllowAdminVideoCreation
        videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals("Video Sec Test"))
                .forEach(videoRepository::delete);
        categoryRepository.deleteById(testCategoryId);
        userRepository.findByEmail(TEST_USER_EMAIL).ifPresent(userRepository::delete);
    }

    // ─────────────────── ENDPOINTS PÚBLICOS ───────────────────────────────

    @Test
    void shouldAllowPublicAccessToVideos() throws Exception {
        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicAccessToMenus() throws Exception {
        mockMvc.perform(get("/menus"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicAccessToCategories() throws Exception {
        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicAccessToCommentsByVideo() throws Exception {
        mockMvc.perform(get("/comments/video/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    // ─────────────────── ADMIN SEM TOKEN ──────────────────────────────────

    @Test
    void shouldBlockAdminVideoCreationWithoutToken() throws Exception {
        VideoRequestDTO request = new VideoRequestDTO(
                "Título", "Desc", "http://url.com", "http://cover.com",
                testCategoryId, null, null, null, null, null, null);

        mockMvc.perform(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockAdminMenuCreationWithoutToken() throws Exception {
        String body = String.format(
                "{\"title\":\"Menu\",\"description\":\"Desc\",\"categoryId\":\"%s\"}",
                testCategoryId);

        mockMvc.perform(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ─────────────────── ADMIN COM ROLE_USER ──────────────────────────────

    @Test
    void shouldBlockAdminVideoCreationWithUserRole() throws Exception {
        VideoRequestDTO request = new VideoRequestDTO(
                "Título", "Desc", "http://url.com", "http://cover.com",
                testCategoryId, null, null, null, null, null, null);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockAdminMenuCreationWithUserRole() throws Exception {
        String body = String.format(
                "{\"title\":\"Menu\",\"description\":\"Desc\",\"categoryId\":\"%s\"}",
                testCategoryId);

        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body), userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────── ADMIN COM ROLE_ADMIN ─────────────────────────────

    @Test
    void shouldAllowAdminVideoCreationWithAdminToken() throws Exception {
        VideoRequestDTO request = new VideoRequestDTO(
                "Video Sec Test", "Desc", "http://url.com", "http://cover.com",
                testCategoryId, null, null, null, null, null, null);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());
    }

    // ─────────────────── FAVORITOS (anyRequest().authenticated()) ─────────

    @Test
    void shouldBlockFavoritesListWithoutToken() throws Exception {
        mockMvc.perform(get("/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockFavoritesByTypeWithoutToken() throws Exception {
        mockMvc.perform(get("/favorites/VIDEO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowFavoritesListWithValidToken() throws Exception {
        mockMvc.perform(bearer(get("/favorites"), adminToken))
                .andExpect(status().isOk());
    }
}
