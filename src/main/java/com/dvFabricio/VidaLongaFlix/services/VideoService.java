//package com.dvFabricio.VidaLongaFlix.services;
//
//import com.dvFabricio.VidaLongaFlix.domain.category.Category;
//import com.dvFabricio.VidaLongaFlix.domain.rating.Rating;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
//import com.dvFabricio.VidaLongaFlix.domain.video.Video;
//import com.dvFabricio.VidaLongaFlix.infra.exception.ResourceNotFoundException;
//import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
//import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//public class VideoService {
//
//    @Autowired
//    private VideoRepository videoRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private StorageService storageService;
//
//    @Transactional
//    public VideoDTO createVideo(VideoDTO videoDTO, MultipartFile file) throws IOException {
//        Video video = new Video();
//        populateVideoDetails(video, videoDTO);
//
//        if (file != null && !file.isEmpty()) {
//            String videoUrl = storageService.uploadFile(file);
//            video.setUrl(videoUrl);
//        }
//
//        video = videoRepository.save(video);
//        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
//    }
//
//    @Transactional
//    public VideoDTO updateVideo(UUID id, VideoDTO videoDTO, MultipartFile file) throws IOException {
//        Video video = videoRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + id));
//        populateVideoDetails(video, videoDTO);
//
//        if (file != null && !file.isEmpty()) {
//            String videoUrl = storageService.uploadFile(file);
//            video.setUrl(videoUrl);
//        }
//
//        video = videoRepository.save(video);
//        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
//    }
//
//    @Transactional(readOnly = true)
//    public Page<VideoDTO> getAllVideos(Pageable pageable) {
//        return videoRepository.findAll(pageable)
//                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
//    }
//
//    @Transactional(readOnly = true)
//    public Page<VideoDTO> getVideosByCategory(String category, Pageable pageable) {
//        return videoRepository.findByCategory_Name(category, pageable)
//                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
//    }
//
//    @Transactional(readOnly = true)
//    public Page<VideoDTO> searchVideosByTitle(String title, Pageable pageable) {
//        return videoRepository.findByTitleContainingIgnoreCase(title, pageable)
//                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
//    }
//
//    @Transactional(readOnly = true)
//    public Page<VideoDTO> getVideosByMinimumScore(int score, Pageable pageable) {
//        return videoRepository.findByRatings_ScoreGreaterThanEqual(score, pageable)
//                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
//    }
//
//    @Transactional(readOnly = true)
//    public VideoDTO getVideoById(UUID id) {
//        Video video = videoRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + id));
//        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
//    }
//
//    @Transactional
//    public void deleteVideo(UUID id) {
//        if (!videoRepository.existsById(id)) {
//            throw new ResourceNotFoundException("Video not found with id " + id);
//        }
//        videoRepository.deleteById(id);
//    }
//
//    private void populateVideoDetails(Video video, VideoDTO videoDTO) {
//        video.setTitle(videoDTO.title());
//        video.setDescription(videoDTO.description());
//        video.setUrl(videoDTO.url());
//
//        Category category = categoryRepository.findByName(videoDTO.categoryName())
//                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + videoDTO.categoryName()));
//        video.setCategory(category);
//    }
//
//}
