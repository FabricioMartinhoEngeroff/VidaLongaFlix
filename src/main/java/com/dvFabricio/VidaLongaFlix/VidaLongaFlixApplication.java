package com.dvFabricio.VidaLongaFlix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VidaLongaFlixApplication {

	public static void main(String[] args) {
		SpringApplication.run(VidaLongaFlixApplication.class, args);
	}

}
