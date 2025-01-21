package com.dvFabricio.VidaLongaFlix.domain.category;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table(name = "categories")
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false, length = 100, unique = true)
    @NotBlank(message = "The category name cannot be empty.")
    @Size(max = 100, message = "The category name cannot exceed 100 characters.")
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<Video> videos = new HashSet<>();

    @Builder
    public Category(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public void addVideo(Video video) {
        if (video != null) {
            this.videos.add(video);
            video.setCategory(this);
        }
    }

    public void removeVideo(Video video) {
        if (video != null) {
            this.videos.remove(video);
            video.setCategory(null);
        }
    }

    public List<String> getVideoTitles() {
        return videos.stream()
                .map(Video::getTitle)
                .collect(Collectors.toList());
    }

    public int getVideoCount() {
        return videos != null ? videos.size() : 0;
    }

}
