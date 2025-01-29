package com.dvFabricio.VidaLongaFlix.domain.video;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "The comment text cannot be empty.")
    @Size(max = 500, message = "The comment text cannot exceed 500 characters.")
    private String text;

    @Column(nullable = false)
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Builder
    public Comment(String text, LocalDateTime date, User user, Video video) {
        this.text = text;
        this.date = date;
        this.user = user;
        this.video = video;
    }

}
