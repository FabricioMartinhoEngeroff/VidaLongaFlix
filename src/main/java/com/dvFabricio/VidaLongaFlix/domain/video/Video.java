package com.dvFabricio.VidaLongaFlix.domain.video;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "The video title cannot be empty.")
    @Size(max = 150, message = "The video title cannot exceed 150 characters.")
    private String title;

    @Column(nullable = false)
    @NotBlank(message = "The video description cannot be empty.")
    private String description;

    @Column(nullable = false)
    @NotBlank(message = "The video URL cannot be empty.")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int views = 0;

    @Column(nullable = false)
    private double watchTime;

    @Builder
    public Video(String title, String description, String url, Category category, int views, double watchTime) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.category = category;
        this.views = views;
        this.watchTime = watchTime;
    }
}


