package com.webphim.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebPhimApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebPhimApiApplication.class, args);
	}

}
