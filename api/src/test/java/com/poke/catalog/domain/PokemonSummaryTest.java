package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonSummaryTest {

	@Test
	void buildsAValidSummary() {
		var pikachu = pikachu(List.of("electric"), List.of(new Ability("static", false)));

		assertThat(pikachu.id()).isEqualTo(25);
		assertThat(pikachu.category()).isEqualTo("Mouse Pokemon");
		assertThat(pikachu.weightHectograms()).isEqualTo(60);
		assertThat(pikachu.abilities()).extracting(Ability::name).containsExactly("static");
	}

	@Test
	void optionalUpstreamFieldsMayBeNull() {
		var form = new PokemonSummary(10001, "deoxys-attack", null, null, 608, 17, List.of(), List.of());

		assertThat(form.spriteUrl()).isNull();
		assertThat(form.category()).isNull();
	}

	@Test
	void listsAreImmutableCopies() {
		var types = new ArrayList<>(List.of("electric"));
		var pikachu = pikachu(types, List.of());
		types.add("fairy");

		assertThat(pikachu.types()).containsExactly("electric");
		assertThatThrownBy(() -> pikachu.types().add("x")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsInvalidIdentityAndMeasures() {
		assertThatThrownBy(() -> new PokemonSummary(0, "x", null, null, 1, 1, List.of(), List.of()))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new PokemonSummary(1, " ", null, null, 1, 1, List.of(), List.of()))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new PokemonSummary(1, "x", null, null, -1, 1, List.of(), List.of()))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new PokemonSummary(1, "x", null, null, 1, -1, List.of(), List.of()))
				.isInstanceOf(DomainValidationException.class);
	}

	private static PokemonSummary pikachu(List<String> types, List<Ability> abilities) {
		return new PokemonSummary(25, "pikachu", "https://img/25.png", "Mouse Pokemon", 60, 4, types, abilities);
	}
}
