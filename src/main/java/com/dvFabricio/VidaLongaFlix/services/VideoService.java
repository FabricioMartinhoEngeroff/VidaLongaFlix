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
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoService(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public VideoDTO create(VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        Category category = categoryRepository.findByUuid(videoDTO.categoryUuid())
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Category with UUID " + videoDTO.categoryUuid() + " not found."
                ));

        try {
            Video video = new Video(
                    videoDTO.title(),
                    videoDTO.description(),
                    videoDTO.url(),
                    category
            );
            video = videoRepository.save(video);
            return new VideoDTO(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving video: " + e.getMessage());
        }
    }

    @Transactional
    public VideoDTO update(UUID uuid, VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        Video video = videoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with UUID " + uuid + " not found."));

        if (videoDTO.categoryUuid() != null) {
            Category category = categoryRepository.findByUuid(videoDTO.categoryUuid())
                    .orElseThrow(() -> new ResourceNotFoundExceptions(
                            "Category with UUID " + videoDTO.categoryUuid() + " not found."
                    ));
            video.setCategory(category);
        }

        try {
            video.update(videoDTO.title(), videoDTO.description(), videoDTO.url());
            video = videoRepository.save(video);
            return new VideoDTO(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while updating video with UUID " + uuid + ": " + e.getMessage());
        }
    }

    public VideoDTO findById(UUID uuid) {
        Video video = videoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with UUID " + uuid + " not found."));
        return new VideoDTO(video);
    }

    public List<VideoDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(VideoDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID uuid) {
        Video video = videoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with UUID " + uuid + " not found."));
        try {
            videoRepository.delete(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting video with UUID " + uuid + ": " + e.getMessage());
        }
    }

    private void validateVideoFields(VideoDTO videoDTO) {
        if (videoDTO.title() == null || videoDTO.title().isBlank()) {
            throw new MissingRequiredFieldException("title", "The video title is required.");
        }
        if (videoDTO.description() == null || videoDTO.description().isBlank()) {
            throw new MissingRequiredFieldException("description", "The video description is required.");
        }
        if (videoDTO.url() == null || videoDTO.url().isBlank()) {
            throw new MissingRequiredFieldException("url", "The video URL is required.");
        }
    }
}
