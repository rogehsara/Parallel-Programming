package com.example.parallel_programming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class ParallelpRogrammingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParallelpRogrammingApplication.class, args);
	}

}
