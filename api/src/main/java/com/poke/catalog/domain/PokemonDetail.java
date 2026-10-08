package com.poke.catalog.domain;

import com.poke.shared.validation.Require;

import java.util.List;

public record PokemonDetail(
		int id,
		String name,
		String imageUrl,
		String category,
		int weightHectograms,
		int heightDecimetres,
		List<String> types,
		List<Ability> abilities,
		List<Stat> stats,
		String description,
		List<EvolutionStage> evolution) {

	public PokemonDetail {
		Require.positive(id, "id");
		Require.text(name, "name");
		Require.nonNegative(weightHectograms, "weight");
		Require.nonNegative(heightDecimetres, "height");
		types = Require.copy(types);
		abilities = Require.copy(abilities);
		stats = Require.copy(stats);
		evolution = Require.copy(evolution);
	}
}
