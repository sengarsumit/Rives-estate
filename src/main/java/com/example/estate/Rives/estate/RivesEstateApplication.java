package com.example.estate.Rives.estate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RivesEstateApplication {

	public static void main(String[] args) {
		SpringApplication.run(RivesEstateApplication.class, args);
	}

}
