package com.dvFabricio.VidaLongaFlix.domain.message;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private String destination;
    private String title;
    private String body;
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    public Message(String destination, String body) {
        this.destination = destination;
        this.body = body;
    }
}
