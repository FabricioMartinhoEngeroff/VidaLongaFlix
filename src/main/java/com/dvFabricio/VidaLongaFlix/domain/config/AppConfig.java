package com.dvFabricio.VidaLongaFlix.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @Id
    @Column(name = "config_key", nullable = false, length = 50)
    private String key;

    @Column(name = "config_value", nullable = false, length = 255)
    private String value;
}
