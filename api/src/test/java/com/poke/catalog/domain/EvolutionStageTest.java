package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvolutionStageTest {

	@Test
	void baseStageHasNoPredecessor() {
		var eevee = new EvolutionStage(0, 133, "eevee", null, null, "https://img/133.png");

		assertThat(eevee.isBase()).isTrue();
		assertThat(eevee.evolvesFromId()).isNull();
	}

	@Test
	void laterStageReferencesItsPredecessor() {
		var vaporeon = new EvolutionStage(1, 134, "vaporeon", 133, "use-item: water-stone", "https://img/134.png");

		assertThat(vaporeon.isBase()).isFalse();
		assertThat(vaporeon.evolvesFromId()).isEqualTo(133);
		assertThat(vaporeon.trigger()).isEqualTo("use-item: water-stone");
	}

	@Test
	void rejectsInconsistentStages() {
		assertThatThrownBy(() -> new EvolutionStage(0, 133, "eevee", 1, null, null))
			.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new EvolutionStage(1, 134, "vaporeon", null, null, null))
			.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new EvolutionStage(-1, 1, "x", null, null, null))
			.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new EvolutionStage(0, 0, "x", null, null, null))
			.isInstanceOf(DomainValidationException.class);
	}
}
