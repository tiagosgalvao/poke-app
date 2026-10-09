package com.poke.localpokemon.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;

final class LocalPokemonResponses {

	private LocalPokemonResponses() {
	}

	record LocalPokemonResponse(
		int id,
		String name,
		String spriteUrl,
		String imageUrl,
		String category,
		double weightKg,
		double heightM,
		List<String> types,
		List<String> abilities,
		String localizedName,
		String region,
		String habitat,
		Set<String> tags,
		String notes,
		long version,
		Instant syncedAt,
		Instant updatedAt) {
	}

	record SyncSummaryResponse(List<Integer> created, List<Integer> refreshed, List<Integer> failed) {
	}
}
