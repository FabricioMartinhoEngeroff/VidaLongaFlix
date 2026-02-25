package com.dvFabricio.VidaLongaFlix.favoriteTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.FavoriteController;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.services.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FavoriteControllerTest {

    private MockMvc mockMvc;

    @InjectMocks private FavoriteController favoriteController;
    @Mock private FavoriteService favoriteService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(favoriteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        user = new User("João", "joao@example.com", "Password1@", "(11) 99999-9999");
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void shouldToggleAndReturnAdded() throws Exception {
        given(favoriteService.toggle(
                nullable(UUID.class), eq("video-1"), eq(FavoriteContentType.VIDEO)))
                .willReturn(true);

        mockMvc.perform(post("/favorites/VIDEO/video-1")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.itemId").value("video-1"))
                .andExpect(jsonPath("$.itemType").value("VIDEO"));
    }

    @Test
    void shouldToggleAndReturnRemoved() throws Exception {
        given(favoriteService.toggle(
                nullable(UUID.class), eq("video-1"), eq(FavoriteContentType.VIDEO)))
                .willReturn(false);

        mockMvc.perform(post("/favorites/VIDEO/video-1")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));
    }

    @Test
    void shouldListAllFavorites() throws Exception {
        List<FavoriteDTO> favorites = List.of(
                new FavoriteDTO("video-1", FavoriteContentType.VIDEO, LocalDateTime.now()),
                new FavoriteDTO("menu-1", FavoriteContentType.MENU, LocalDateTime.now())
        );
        given(favoriteService.listAll(nullable(UUID.class))).willReturn(favorites);

        mockMvc.perform(get("/favorites")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void shouldListFavoritesByType() throws Exception {
        List<FavoriteDTO> favorites = List.of(
                new FavoriteDTO("video-1", FavoriteContentType.VIDEO, LocalDateTime.now())
        );
        given(favoriteService.listByType(
                nullable(UUID.class), eq(FavoriteContentType.VIDEO)))
                .willReturn(favorites);

        mockMvc.perform(get("/favorites/VIDEO")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].itemId").value("video-1"));
    }

    @Test
    void shouldReturnFavoriteStatus() throws Exception {
        given(favoriteService.isFavorited(
                nullable(UUID.class), eq("video-1"), eq(FavoriteContentType.VIDEO)))
                .willReturn(true);
        given(favoriteService.countLikes(eq("video-1"), eq(FavoriteContentType.VIDEO)))
                .willReturn(5L);

        mockMvc.perform(get("/favorites/VIDEO/video-1/status")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.likesCount").value(5));
    }

    @Test
    void shouldReturnEmptyListWhenNoFavorites() throws Exception {
        given(favoriteService.listAll(nullable(UUID.class))).willReturn(List.of());

        mockMvc.perform(get("/favorites")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }}