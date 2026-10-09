package com.poke.localpokemon.controller;

import com.poke.localpokemon.controller.LocalPokemonResponses.LocalPokemonResponse;
import com.poke.localpokemon.controller.LocalPokemonResponses.SyncSummaryResponse;
import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.SyncSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class LocalPokemonResponseMapperTest {

	private final LocalPokemonResponseMapper mapper = new LocalPokemonResponseMapperImpl();

	@Test
	void flattensUpstreamAndProprietaryDataAndConvertsMeasures() {
		var pokemon = new LocalPokemon(25, pikachuUpstream(), pikachuProprietary(), 3, IMPORTED_AT, LATER);

		var response = mapper.toResponse(pokemon);

		assertThat(response).isEqualTo(new LocalPokemonResponse(25, "pikachu", "https://img/25.png",
			"https://img/art/25.png", "Mouse Pokemon", 6.0, 0.4, List.of("electric"), List.of("static", "lightning-rod"),
			"ピカチュウ", "Kanto", "forest", Set.of("starter", "mascot"), "Ash's partner", 3, IMPORTED_AT, LATER));
	}

	@Test
	void mapsTheSyncSummary() {
		var summary = new SyncSummary(List.of(1), List.of(4), List.of(10_000));

		assertThat(mapper.toSyncResponse(summary)).isEqualTo(new SyncSummaryResponse(List.of(1), List.of(4), List.of(10_000)));
	}
}
