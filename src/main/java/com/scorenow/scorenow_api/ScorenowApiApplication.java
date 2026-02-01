package com.scorenow.scorenow_api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScorenowApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(ScorenowApiApplication.class, args);
	}

}