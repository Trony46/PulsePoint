package com.pulsepoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PulsepointApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulsepointApplication.class, args);
	}

}
