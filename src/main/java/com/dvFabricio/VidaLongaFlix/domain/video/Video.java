package com.dvFabricio.VidaLongaFlix.domain.video;

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
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
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

    @Column(columnDefinition = "TEXT")
    private String receita;

    private Double proteinas;
    private Double carboidratos;
    private Double gorduras;
    private Double fibras;


    @Builder
    public Video(String title, String description, String url, Category category, int views, double watchTime,
                 String receita, Double proteinas, Double carboidratos, Double gorduras, Double fibras) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.category = category;
        this.views = views;
        this.watchTime = watchTime;
        this.receita = receita;
        this.proteinas = proteinas;
        this.carboidratos = carboidratos;
        this.gorduras = gorduras;
        this.fibras = fibras;
    }
}



