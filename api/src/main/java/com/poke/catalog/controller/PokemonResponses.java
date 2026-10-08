package com.poke.catalog.controller;

import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.EvolutionStage;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.catalog.domain.Stat;

import java.util.List;

import static com.poke.shared.measure.Measures.kilograms;
import static com.poke.shared.measure.Measures.metres;

final class PokemonResponses {

	private PokemonResponses() {
	}

	record AbilityResponse(String name, boolean hidden) {
		static AbilityResponse from(Ability ability) {
			return new AbilityResponse(ability.name(), ability.hidden());
		}
	}

	record StatResponse(String name, int value) {
		static StatResponse from(Stat stat) {
			return new StatResponse(stat.name(), stat.value());
		}
	}

	record EvolutionStageResponse(int stage, int id, String name, Integer evolvesFromId, String trigger, String spriteUrl) {
		static EvolutionStageResponse from(EvolutionStage stage) {
			return new EvolutionStageResponse(stage.stage(), stage.id(), stage.name(), stage.evolvesFromId(),
					stage.trigger(), stage.spriteUrl());
		}
	}

	record PokemonSummaryResponse(
			int id,
			String name,
			String spriteUrl,
			String category,
			double weightKg,
			double heightM,
			List<String> types,
			List<AbilityResponse> abilities) {

		static PokemonSummaryResponse from(PokemonSummary pokemon) {
			return new PokemonSummaryResponse(
					pokemon.id(),
					pokemon.name(),
					pokemon.spriteUrl(),
					pokemon.category(),
					kilograms(pokemon.weightHectograms()),
					metres(pokemon.heightDecimetres()),
					pokemon.types(),
					pokemon.abilities().stream().map(AbilityResponse::from).toList());
		}
	}

	record PokemonDetailResponse(
			int id,
			String name,
			String spriteUrl,
			String imageUrl,
			String category,
			double weightKg,
			double heightM,
			List<String> types,
			List<AbilityResponse> abilities,
			List<StatResponse> stats,
			String description,
			List<EvolutionStageResponse> evolution) {

		static PokemonDetailResponse from(PokemonDetail pokemon) {
			return new PokemonDetailResponse(
					pokemon.id(),
					pokemon.name(),
					pokemon.spriteUrl(),
					pokemon.imageUrl(),
					pokemon.category(),
					kilograms(pokemon.weightHectograms()),
					metres(pokemon.heightDecimetres()),
					pokemon.types(),
					pokemon.abilities().stream().map(AbilityResponse::from).toList(),
					pokemon.stats().stream().map(StatResponse::from).toList(),
					pokemon.description(),
					pokemon.evolution().stream().map(EvolutionStageResponse::from).toList());
		}
	}
}
