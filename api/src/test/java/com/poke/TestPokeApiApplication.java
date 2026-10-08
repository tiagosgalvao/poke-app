package com.poke;

import org.springframework.boot.SpringApplication;

public class TestPokeApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(PokeApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
