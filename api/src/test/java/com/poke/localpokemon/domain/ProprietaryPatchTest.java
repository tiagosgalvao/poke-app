package com.poke.localpokemon.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuProprietary;
import static org.assertj.core.api.Assertions.assertThat;

class ProprietaryPatchTest {

	@Test
	void replacesOnlyTheFieldsThatArePresent() {
		var patch = new ProprietaryPatch(null, "Johto", null, null, null);

		var patched = patch.applyTo(pikachuProprietary());

		assertThat(patched.region()).isEqualTo("Johto");
		assertThat(patched.localizedName()).isEqualTo("ピカチュウ");
		assertThat(patched.habitat()).isEqualTo("forest");
		assertThat(patched.tags()).containsExactly("mascot", "starter");
		assertThat(patched.notes()).isEqualTo("Ash's partner");
	}

	@Test
	void replacesTagsAsAWholeWhenPresent() {
		var patched = new ProprietaryPatch(null, null, null, Set.of("electric-mouse"), null).applyTo(pikachuProprietary());

		assertThat(patched.tags()).containsExactly("electric-mouse");
	}

	@Test
	void anEmptyPatchChangesNothing() {
		var current = pikachuProprietary();

		assertThat(new ProprietaryPatch(null, null, null, null, null).applyTo(current)).isEqualTo(current);
	}
}
