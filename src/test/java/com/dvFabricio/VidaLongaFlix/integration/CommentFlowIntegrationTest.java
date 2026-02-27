package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.comment.CreateCommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
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
 * Testes de integração para o fluxo completo de comentários.
 * <p>
 * NOTA SOBRE SEGURANÇA:
 * O endpoint POST /comments usa @AuthenticationPrincipal User user e chama user.getId()
 * sem verificar se user é null. O path /comments/** está configurado como permitAll()
 * no SecurityConfig, o que significa que uma chamada sem token chegará ao controller
 * com user == null e causará NullPointerException (500). Isso é documentado abaixo.
 * A correção recomendada é mover POST /comments para fora de /comments/** no SecurityConfig
 * ou adicionar verificação de null no controller.
 */
class CommentFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CommentRepository commentRepository;

    private String adminToken;
    private UUID videoId;
    private UUID categoryId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();

        // Cria categoria + vídeo diretamente via repositório para setup rápido
        Category category = categoryRepository.save(
                new Category("Comment-Flow-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        categoryId = category.getId();

        Video video = Video.builder()
                .title("Comment-Flow-Video-" + UUID.randomUUID())
                .description("Vídeo para teste de comentários")
                .url("https://youtube.com/test-comment")
                .cover("https://img.test/comment-cover.jpg")
                .category(category)
                .build();
        videoRepository.save(video);
        videoId = video.getId();
    }

    @AfterEach
    void cleanup() {
        // Comentários primeiro (FK para vídeo e usuário)
        commentRepository.findByVideo_Id(videoId).forEach(commentRepository::delete);
        videoRepository.deleteById(videoId);
        categoryRepository.deleteById(categoryId);
    }

    // ─────────────────────────── GET (público) ────────────────────────────

    @Test
    void shouldReturnEmptyCommentListForVideoWithNoComments() throws Exception {
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnEmptyListForNonExistentVideo() throws Exception {
        // O CommentService retorna lista vazia para videoId que não existe
        mockMvc.perform(get("/comments/video/{videoId}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────── POST (autenticado) ───────────────────────

    @Test
    void shouldCreateCommentAsAuthenticatedUser() throws Exception {
        CreateCommentDTO request = new CreateCommentDTO("Excelente conteúdo!", videoId);

        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        // Verifica que o comentário foi salvo
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Excelente conteúdo!"));
    }

    @Test
    void shouldReturnBadRequestWhenCommentTextIsBlank() throws Exception {
        String body = String.format("{\"text\":\"\",\"videoId\":\"%s\"}", videoId);

        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body), adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDocumentSecurityGapWhenCreatingCommentWithoutToken() throws Exception {
        // FALHA ESPERADA DE SEGURANÇA:
        // /comments/** é permitAll no SecurityConfig. Sem token, o user é null.
        // O controller faz user.getId() sem null-check → NullPointerException → 500.
        // Recomendação: proteger POST /comments separadamente no SecurityConfig.
        CreateCommentDTO request = new CreateCommentDTO("Comentário sem auth", videoId);

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError()); // NullPointerException
    }

    // ─────────────────────────── DELETE ───────────────────────────────────

    @Test
    void shouldDeleteCommentSuccessfully() throws Exception {
        // Cria o comentário
        CreateCommentDTO request = new CreateCommentDTO("Comentário para deletar", videoId);
        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        UUID commentId = commentRepository.findByVideo_Id(videoId)
                .stream().findFirst().orElseThrow().getId();

        mockMvc.perform(delete("/comments/{id}", commentId))
                .andExpect(status().isNoContent());

        // Verifica remoção
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentComment() throws Exception {
        mockMvc.perform(delete("/comments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────── DUPLICATA ───────────────────────────────

    @Test
    void shouldReturn409WhenCreatingDuplicateCommentByTheSameUserOnTheSameVideo()
            throws Exception {
        // Cria o comentário uma primeira vez
        CreateCommentDTO request = new CreateCommentDTO("Comentário duplicado", videoId);
        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        // Tenta criar o mesmo comentário novamente
        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isConflict());
    }
}
