package com.poke.localpokemon.domain;

import java.util.Set;

public record ProprietaryPatch(String localizedName, String region, String habitat, Set<String> tags, String notes) {

	public ProprietaryData applyTo(ProprietaryData current) {
		return new ProprietaryData(
				valueOrCurrent(localizedName, current.localizedName()),
				valueOrCurrent(region, current.region()),
				valueOrCurrent(habitat, current.habitat()),
				valueOrCurrent(tags, current.tags()),
				valueOrCurrent(notes, current.notes()));
	}

	private static <T> T valueOrCurrent(T value, T current) {
		return value == null ? current : value;
	}
}
