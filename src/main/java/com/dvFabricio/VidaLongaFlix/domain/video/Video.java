package com.dvFabricio.VidaLongaFlix.domain.video;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
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
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String url;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments;

    public String getCategoryName() {
        return category != null ? category.getName() : "Categoria não disponível";
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
