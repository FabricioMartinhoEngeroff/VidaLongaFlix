package com.dvFabricio.VidaLongaFlix.domain.message;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String destino;
    private String titulo;
    private String corpo;
    private String statusEntrega;

    public Message(String destino, String corpo) {
        this.destino = destino;
        this.corpo = corpo;
    }
}

