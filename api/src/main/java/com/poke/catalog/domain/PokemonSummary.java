package com.poke.catalog.domain;

import com.poke.shared.validation.Require;

import java.util.List;

public record PokemonSummary(
	int id,
	String name,
	String spriteUrl,
	String category,
	int weightHectograms,
	int heightDecimetres,
	List<String> types,
	List<Ability> abilities) {

	public PokemonSummary {
		Require.positive(id, "id");
		Require.text(name, "name");
		Require.nonNegative(weightHectograms, "weight");
		Require.nonNegative(heightDecimetres, "height");
		types = Require.copy(types);
		abilities = Require.copy(abilities);
	}
}
