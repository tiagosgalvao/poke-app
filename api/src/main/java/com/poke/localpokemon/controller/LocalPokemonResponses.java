package com.poke.localpokemon.controller;

import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.SyncSummary;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.poke.shared.measure.Measures.kilograms;
import static com.poke.shared.measure.Measures.metres;

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

		static LocalPokemonResponse from(LocalPokemon pokemon) {
			var upstream = pokemon.upstream();
			var proprietary = pokemon.proprietary();
			return new LocalPokemonResponse(
					pokemon.id(),
					upstream.name(),
					upstream.spriteUrl(),
					upstream.imageUrl(),
					upstream.category(),
					kilograms(upstream.weightHectograms()),
					metres(upstream.heightDecimetres()),
					upstream.types(),
					upstream.abilities(),
					proprietary.localizedName(),
					proprietary.region(),
					proprietary.habitat(),
					proprietary.tags(),
					proprietary.notes(),
					pokemon.version(),
					pokemon.syncedAt(),
					pokemon.updatedAt());
		}
	}

	record SyncSummaryResponse(List<Integer> created, List<Integer> refreshed, List<Integer> failed) {

		static SyncSummaryResponse from(SyncSummary summary) {
			return new SyncSummaryResponse(summary.created(), summary.refreshed(), summary.failed());
		}
	}
}
