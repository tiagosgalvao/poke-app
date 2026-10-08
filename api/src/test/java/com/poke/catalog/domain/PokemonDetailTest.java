package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonDetailTest {

	@Test
	void buildsAValidDetail() {
		var detail = new PokemonDetail(25, "pikachu", "https://img/25.png", "https://img/art/25.png", "Mouse Pokemon", 60, 4,
				List.of("electric"), List.of(new Ability("static", false)), List.of(new Stat("speed", 90)),
				"Possesses cheek sacs.", List.of(new EvolutionStage(0, 172, "pichu", null, null, null)));

		assertThat(detail.stats()).extracting(Stat::value).containsExactly(90);
		assertThat(detail.description()).isEqualTo("Possesses cheek sacs.");
		assertThat(detail.evolution()).extracting(EvolutionStage::name).containsExactly("pichu");
	}

	@Test
	void nullListsBecomeEmpty() {
		var detail = new PokemonDetail(25, "pikachu", null, null, null, 60, 4, null, null, null, null, null);

		assertThat(detail.types()).isEmpty();
		assertThat(detail.abilities()).isEmpty();
		assertThat(detail.stats()).isEmpty();
		assertThat(detail.evolution()).isEmpty();
	}

	@Test
	void rejectsInvalidIdentity() {
		assertThatThrownBy(() -> new PokemonDetail(-5, "x", null, null, null, 1, 1, null, null, null, null, null))
				.isInstanceOf(DomainValidationException.class);
	}
}
