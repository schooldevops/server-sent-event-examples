package com.schooldevops.sse.ssedemo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalTime;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class SseDemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SseDemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		WebClient client = WebClient.create("http://localhost:8080/sse");
		ParameterizedTypeReference<ServerSentEvent<String>> type
				= new ParameterizedTypeReference<ServerSentEvent<String>>() {};

		Flux<ServerSentEvent<String>> eventStream = client.get()
				.uri("")
				.retrieve()
				.bodyToFlux(type);

		eventStream.subscribe(
				content -> log.info("Time: {} - event: name[{}], id [{}], content[{}] ",
						LocalTime.now(), content.event(), content.id(), content.data()),
				error -> log.error("Error receiving SSE: {}", error),
				() -> log.info("Completed!!!"));
	}
}
