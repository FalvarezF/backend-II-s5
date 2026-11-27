package com.petsalud.backendpetsalud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class BackendpetsaludApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendpetsaludApplication.class, args);
	}

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
