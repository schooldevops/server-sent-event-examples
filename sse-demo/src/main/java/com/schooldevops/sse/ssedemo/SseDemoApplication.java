package com.schooldevops.sse.ssedemo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SseDemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SseDemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
