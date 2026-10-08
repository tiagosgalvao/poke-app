package com.poke.catalog.client.dto;

import com.poke.catalog.client.MalformedPokeApiResponseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceUrlsTest {

	@Test
	void extractsTheTrailingId() {
		assertThat(ResourceUrls.idOf("https://pokeapi.co/api/v2/pokemon-species/25/")).isEqualTo(25);
		assertThat(ResourceUrls.idOf("https://pokeapi.co/api/v2/evolution-chain/67")).isEqualTo(67);
	}

	@Test
	void rejectsMissingOrMalformedUrls() {
		assertThatThrownBy(() -> ResourceUrls.idOf(null)).isInstanceOf(MalformedPokeApiResponseException.class);
		assertThatThrownBy(() -> ResourceUrls.idOf("https://pokeapi.co/api/v2/pokemon/pikachu/"))
				.isInstanceOf(MalformedPokeApiResponseException.class);
	}
}
