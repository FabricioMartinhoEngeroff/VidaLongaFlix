package com.dvFabricio.VidaLongaFlix.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final String storagePath;

    public MediaResourceConfig(
            @Value("${media.storage.path:${java.io.tmpdir}/vidalongaflix-media}") String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(Paths.get(storagePath).toAbsolutePath().normalize().toUri().toString());
    }
}
