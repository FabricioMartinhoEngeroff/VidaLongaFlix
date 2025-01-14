//package com.dvFabricio.VidaLongaFlix.controllers;
//
//import com.dvFabricio.VidaLongaFlix.domain.video.Video;
//import com.dvFabricio.VidaLongaFlix.services.VideoService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/videos")
//public class VideoController {
//
//    @Autowired
//    private VideoService videoService;
//
//    @PostMapping
//    public ResponseEntity<Video> createVideo(@RequestPart("video") Video video, @RequestPart("file") MultipartFile file) {
//        try {
//            Video createdVideo = videoService.createVideo(video, file);
//            return ResponseEntity.ok(createdVideo);
//        } catch (IOException e) {
//            return ResponseEntity.status(500).build();
//        }
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Video>> getAllVideos() {
//        List<Video> videos = videoService.getAllVideos();
//        return ResponseEntity.ok(videos);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Video> getVideoById(@PathVariable Long id) {
//        Video video = videoService.getVideoById(id);
//        if (video != null) {
//            return ResponseEntity.ok(video);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
//        videoService.deleteVideo(id);
//        return ResponseEntity.noContent().build();
//    }
//}