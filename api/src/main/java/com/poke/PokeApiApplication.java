package com.poke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PokeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokeApiApplication.class, args);
	}

}
