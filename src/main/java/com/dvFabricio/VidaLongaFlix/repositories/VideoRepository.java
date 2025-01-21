package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findByCategory_Name(String categoryName, Pageable pageable);

    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Video> findByRatings_ScoreGreaterThanEqual(int score, Pageable pageable);

    Optional<Video> findByUuid(UUID uuid);
}