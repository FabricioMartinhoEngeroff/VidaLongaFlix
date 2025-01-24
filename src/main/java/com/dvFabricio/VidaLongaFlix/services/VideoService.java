package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoService(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void create(VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        Video video = Video.builder()
                .title(videoDTO.title())
                .description(videoDTO.description())
                .url(videoDTO.url())
                .category(findCategoryById(videoDTO.categoryId()))
                .build();

        saveVideo(video);
    }

    @Transactional
    public void update(UUID id, VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        Video video = findVideoById(id);

        if (videoDTO.categoryId() != null) {
            video.setCategory(findCategoryById(videoDTO.categoryId()));
        }

        video.update(videoDTO.title(), videoDTO.description(), videoDTO.url());
        saveVideo(video);
    }

    public VideoDTO findById(UUID id) {
        return new VideoDTO(findVideoById(id));
    }

    public List<VideoDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(VideoDTO::new)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        Video video = findVideoById(id);
        try {
            videoRepository.delete(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting video with ID " + id + ": " + e.getMessage());
        }
    }

    private void saveVideo(Video video) {
        try {
            videoRepository.save(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving video: " + e.getMessage());
        }
    }

    private Video findVideoById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with ID " + id + " not found."));
    }

    private Category findCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with ID " + categoryId + " not found."));
    }

    private void validateVideoFields(VideoDTO videoDTO) {
        if (isBlank(videoDTO.title())) {
            throw new MissingRequiredFieldException("title", "The video title is required.");
        }
        if (isBlank(videoDTO.description())) {
            throw new MissingRequiredFieldException("description", "The video description is required.");
        }
        if (isBlank(videoDTO.url())) {
            throw new MissingRequiredFieldException("url", "The video URL is required.");
        }
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}