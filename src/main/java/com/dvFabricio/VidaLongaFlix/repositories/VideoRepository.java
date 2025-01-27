package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    @Query("SELECT v FROM Video v ORDER BY v.views DESC")
    List<Video> findTopByOrderByViewsDesc(Pageable limit);

    @Query("SELECT v FROM Video v ORDER BY v.views ASC")
    List<Video> findTopByOrderByViewsAsc(Pageable limit);


    @Query("SELECT SUM(v.views) FROM Video v WHERE v.category.id = :categoryId")
    Long countViewsByCategoryId(@Param("categoryId") UUID categoryId);


    @Query("SELECT AVG(v.watchTime) FROM Video v WHERE v.id = :videoId")
    Optional<Double> findAverageWatchTimeByVideoId(@Param("videoId") UUID videoId);


    @Query("SELECT v FROM Video v LEFT JOIN v.comments c GROUP BY v ORDER BY COUNT(c) DESC")
    List<Video> findTopByOrderByCommentsCountDesc(Pageable limit);
}
