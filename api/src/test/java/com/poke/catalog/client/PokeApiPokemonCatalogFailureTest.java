package com.poke.catalog.client;

import com.poke.catalog.client.dto.NamedResource;
import com.poke.catalog.client.dto.PokemonListDto;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PokeApiPokemonCatalogFailureTest {

	@Test
	void anUnexpectedFailureWhileFetchingAPageBecomesUnavailable() {
		var client = mock(PokeApiClient.class);
		when(client.list(0, 1)).thenReturn(new PokemonListDto(1, List.of(
				new NamedResource("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"))));
		when(client.pokemon("1")).thenThrow(new IllegalStateException("connection pool exhausted"));

		assertThatThrownBy(() -> new PokeApiPokemonCatalog(client).findPage(new PageRequest(0, 1)))
				.isInstanceOf(ExternalServiceUnavailableException.class)
				.hasCauseInstanceOf(IllegalStateException.class);
	}
}
