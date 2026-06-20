package com.example.parallel_programming;

import com.example.parallel_programming.dto.BatchSummary;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class ParallelpRogrammingApplication {

	String SUMMARY_FILE_PATH = "batch-summary.json";

	public static void main(String[] args) {
		SpringApplication.run(ParallelpRogrammingApplication.class, args);
	}

	@Bean
	ApplicationRunner init(){
		return args -> {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registeredModules();

			BatchSummary summary = new BatchSummary(0, 0, 0.0, null);

			objectMapper
					.writerWithDefaultPrettyPrinter()
					.writeValue(new File(SUMMARY_FILE_PATH), summary);

		};
	}

}
