package com.poke.localpokemon.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class LocalPokemonFixtures {

	public static final Instant IMPORTED_AT = Instant.parse("2026-10-08T10:00:00Z");
	public static final Instant LATER = Instant.parse("2026-10-08T12:00:00Z");

	private LocalPokemonFixtures() {
	}

	public static UpstreamData pikachuUpstream() {
		return new UpstreamData("pikachu", "https://img/25.png", "https://img/art/25.png", "Mouse Pokemon", 60, 4,
				List.of("electric"), List.of("static", "lightning-rod"));
	}

	public static ProprietaryData pikachuProprietary() {
		return new ProprietaryData("ピカチュウ", "Kanto", "forest", Set.of("starter", "mascot"), "Ash's partner");
	}

	public static LocalPokemon importedPikachu() {
		return LocalPokemon.importFrom(25, pikachuUpstream(), IMPORTED_AT);
	}
}
