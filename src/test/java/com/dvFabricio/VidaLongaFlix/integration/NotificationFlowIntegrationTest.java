package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.notification.Notification;
import com.dvFabricio.VidaLongaFlix.domain.notification.NotificationType;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.NotificationRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o sistema de notificações.
 * <p>
 * Cobre: autenticação, contagem de não-lidos, marcar como lido e paginação.
 */
class NotificationFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;

    private String adminToken;
    private UUID categoryId;
    private UUID createdVideoId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
        notificationRepository.deleteAll();

        Category category = categoryRepository.save(
                new Category("Notif-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        categoryId = category.getId();
    }

    @AfterEach
    void cleanup() {
        notificationRepository.deleteAll();
        if (createdVideoId != null) {
            videoRepository.deleteById(createdVideoId);
            createdVideoId = null;
        }
        if (categoryId != null) {
            categoryRepository.deleteById(categoryId);
        }
    }

    // --- Autenticação ---

    @Test
    void shouldReturn403WhenGettingNotificationsWithoutToken() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenGettingUnreadCountWithoutToken() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenMarkingAllReadWithoutToken() throws Exception {
        mockMvc.perform(post("/notifications/mark-all-read"))
                .andExpect(status().isForbidden());
    }

    // --- Contagem de não-lidos ---

    @Test
    void unreadCountShouldBeZeroWhenNoNotificationsExist() throws Exception {
        mockMvc.perform(bearer(get("/notifications/unread-count"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void unreadCountShouldIncreaseAfterVideoCreation() throws Exception {
        // Inicialmente 0
        mockMvc.perform(bearer(get("/notifications/unread-count"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // Criar vídeo via API (deve disparar uma notificação)
        createVideoViaApi("Notif Increment Test " + UUID.randomUUID());

        // Deve ser 1
        mockMvc.perform(bearer(get("/notifications/unread-count"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    // --- Marcar como lido ---

    @Test
    void markAllReadShouldResetUnreadCount() throws Exception {
        // Criar uma notificação diretamente
        saveNotification("Vídeo de Saúde", NotificationType.VIDEO);

        // Verificar que há 1 não-lida
        mockMvc.perform(bearer(get("/notifications/unread-count"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Marcar tudo como lido
        mockMvc.perform(bearer(post("/notifications/mark-all-read"), adminToken))
                .andExpect(status().isNoContent());

        // Contador deve ser 0
        mockMvc.perform(bearer(get("/notifications/unread-count"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    // --- Listagem e paginação ---

    @Test
    void notificationsListShouldReturnItemsWithReadStatus() throws Exception {
        saveNotification("Receita Fit", NotificationType.MENU);

        mockMvc.perform(bearer(get("/notifications"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Receita Fit"))
                .andExpect(jsonPath("$.items[0].type").value("MENU"))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void notificationsListShouldSupportPagination() throws Exception {
        // Salvar 3 notificações
        for (int i = 1; i <= 3; i++) {
            saveNotification("Notificação " + i, NotificationType.VIDEO);
        }

        // Página 0, tamanho 2 → hasMore = true
        mockMvc.perform(bearer(get("/notifications")
                        .param("page", "0").param("size", "2"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true));

        // Página 0, tamanho 10 → hasMore = false
        mockMvc.perform(bearer(get("/notifications")
                        .param("page", "0").param("size", "10"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void itemsMarkedAsReadAfterMarkAllRead() throws Exception {
        saveNotification("Vídeo Novo", NotificationType.VIDEO);

        // Marcar como lido
        mockMvc.perform(bearer(post("/notifications/mark-all-read"), adminToken))
                .andExpect(status().isNoContent());

        // A notificação existente deve aparecer como lida
        mockMvc.perform(bearer(get("/notifications"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    // --- Helpers ---

    private void saveNotification(String title, NotificationType type) {
        Notification n = new Notification();
        n.setType(type);
        n.setTitle(title);
        n.setContentId(UUID.randomUUID());
        notificationRepository.save(n);
    }

    private void createVideoViaApi(String title) throws Exception {
        VideoRequestDTO req = new VideoRequestDTO(
                title, "Descrição de teste",
                "https://youtube.com/test-" + UUID.randomUUID(),
                "https://img.test/cover.jpg",
                categoryId, null, null, null, null, null, null);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)), adminToken))
                .andExpect(status().isCreated());

        createdVideoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals(title))
                .findFirst().orElseThrow().getId();
    }
}
