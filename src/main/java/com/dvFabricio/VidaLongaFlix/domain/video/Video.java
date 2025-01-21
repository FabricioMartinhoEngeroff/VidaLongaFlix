package com.dvFabricio.VidaLongaFlix.domain.video;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@Table(name = "videos")
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID uuid = UUID.randomUUID();

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

    @Builder
    public Video(String title, String description, String url, Category category) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.category = category;
    }

    public void update(String title, String description, String url) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (url != null && !url.isBlank()) {
            this.url = url;
        }
    }

    public List<CommentDTO> getCommentDTOs() {
        return comments.stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }

    public int getCommentCount() {
        return comments != null ? comments.size() : 0;
    }

    public List<String> getUserNamesFromComments() {
        return comments.stream()
                .map(comment -> comment.getUser().getLogin())
                .collect(Collectors.toList());
    }
}
