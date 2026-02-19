package com.dvFabricio.VidaLongaFlix.domain.video;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String url;

    @Column(name = "thumbnail_url")
    private String cover;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int views = 0;

    @Column(nullable = false)
    private double watchTime = 0.0;

    @Column(columnDefinition = "TEXT")
    private String recipe;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "fat")
    private Double fat;

    @Column(name = "fiber")
    private Double fiber;

    @Column(name = "calories")
    private Double calories;

    @Column(nullable = false)
    private int likesCount = 0;

    @Column(nullable = false)
    private boolean favorited = false;

    @Builder
    public Video(String title, String description, String url, String cover,
                 Category category, int views, double watchTime,
                 String recipe, Double protein, Double carbs, Double fat,
                 Double fiber, Double calories, int likesCount, boolean favorited) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.cover = cover;
        this.category = category;
        this.views = views;
        this.watchTime = watchTime;
        this.recipe = recipe;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.calories = calories;
        this.likesCount = likesCount;
        this.favorited = favorited;
    }
}

