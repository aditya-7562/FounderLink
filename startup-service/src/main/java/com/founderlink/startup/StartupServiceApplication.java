package com.founderlink.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StartupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StartupServiceApplication.class, args);
		System.out.println("Startup Service is running!!!!");
	}
}
