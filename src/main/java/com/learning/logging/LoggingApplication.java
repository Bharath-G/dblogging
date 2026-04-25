package com.learning.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoggingApplication {

	static void main(String[] args) {
		SpringApplication.run(LoggingApplication.class, args);
	}

	private LoggingApplication(){
		//not needed
	}
}
