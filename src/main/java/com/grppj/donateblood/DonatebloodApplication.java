package com.grppj.donateblood;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class DonatebloodApplication {

	public static void main(String[] args) {
		SpringApplication.run(DonatebloodApplication.class, args);
	}

}
