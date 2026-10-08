package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

public record EvolutionStage(int stage, int id, String name, Integer evolvesFromId, String trigger, String spriteUrl) {

	public EvolutionStage {
		Require.nonNegative(stage, "stage");
		Require.positive(id, "id");
		Require.text(name, "name");
		if (stage == 0 && evolvesFromId != null) {
			throw new DomainValidationException("the base stage cannot evolve from another Pokemon");
		}
		if (stage > 0 && evolvesFromId == null) {
			throw new DomainValidationException("stage " + stage + " must reference the Pokemon it evolves from");
		}
	}

	public boolean isBase() {
		return stage == 0;
	}
}
