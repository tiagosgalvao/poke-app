package com.poke.catalog.controller;

import java.util.List;

final class PokemonResponses {

	private PokemonResponses() {
	}

	record AbilityResponse(String name, boolean hidden) {
	}

	record StatResponse(String name, int value) {
	}

	record EvolutionStageResponse(int stage,
								  int id,
								  String name,
								  Integer evolvesFromId,
								  String trigger,
								  String spriteUrl) {
	}

	record PokemonSummaryResponse(int id,
								  String name,
								  String spriteUrl,
								  String category,
								  double weightKg,
								  double heightM,
								  List<String> types,
								  List<AbilityResponse> abilities) {
	}

	record PokemonDetailResponse(int id,
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
	}
}
