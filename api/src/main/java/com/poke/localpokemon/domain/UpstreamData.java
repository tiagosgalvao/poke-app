package com.poke.localpokemon.domain;

import com.poke.shared.validation.Require;

import java.util.List;

public record UpstreamData(
	String name,
	String spriteUrl,
	String imageUrl,
	String category,
	int weightHectograms,
	int heightDecimetres,
	List<String> types,
	List<String> abilities) {

	public UpstreamData {
		Require.text(name, "name");
		Require.nonNegative(weightHectograms, "weight");
		Require.nonNegative(heightDecimetres, "height");
		types = Require.copy(types);
		abilities = Require.copy(abilities);
	}
}
