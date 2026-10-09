package com.poke.localpokemon.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuUpstream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpstreamDataTest {

	@Test
	void keepsTheCopiedPokeApiFields() {
		var pikachu = pikachuUpstream();

		assertThat(pikachu.name()).isEqualTo("pikachu");
		assertThat(pikachu.weightHectograms()).isEqualTo(60);
		assertThat(pikachu.types()).containsExactly("electric");
		assertThat(pikachu.abilities()).containsExactly("static", "lightning-rod");
	}

	@Test
	void listsAreImmutableCopiesAndNullBecomesEmpty() {
		var types = new ArrayList<>(List.of("electric"));
		var pikachu = new UpstreamData("pikachu", null, null, null, 60, 4, types, null);
		types.add("fairy");

		assertThat(pikachu.types()).containsExactly("electric");
		assertThat(pikachu.abilities()).isEmpty();
	}

	@Test
	void rejectsABlankNameAndNegativeMeasures() {
		assertThatThrownBy(() -> new UpstreamData(" ", null, null, null, 60, 4, List.of(), List.of()))
			.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new UpstreamData("pikachu", null, null, null, -1, 4, List.of(), List.of()))
			.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new UpstreamData("pikachu", null, null, null, 60, -1, List.of(), List.of()))
			.isInstanceOf(DomainValidationException.class);
	}
}
