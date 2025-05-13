package com.flyby.ramble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RambleApplication {

	public static void main(String[] args) {
		SpringApplication.run(RambleApplication.class, args);
	}

}
