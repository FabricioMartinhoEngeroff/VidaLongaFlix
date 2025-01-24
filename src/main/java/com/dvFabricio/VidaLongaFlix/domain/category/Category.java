package com.dvFabricio.VidaLongaFlix.domain.category;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "categories")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    @NotBlank(message = "The category name cannot be empty.")
    @Size(max = 100, message = "The category name cannot exceed 100 characters.")
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<Video> videos = new HashSet<>();


    public Category(String name) {
        this.name = name;
    }

}