package com.poke.catalog.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("poke.pokeapi")
public record PokeApiProperties(
		@DefaultValue("https://pokeapi.co/api/v2") String baseUrl,
		@DefaultValue("2s") Duration connectTimeout,
		@DefaultValue("5s") Duration readTimeout) {
}
