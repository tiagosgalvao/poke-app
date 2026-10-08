package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityTest {

	@Test
	void keepsNameAndHiddenFlag() {
		var ability = new Ability("lightning-rod", true);

		assertThat(ability.name()).isEqualTo("lightning-rod");
		assertThat(ability.hidden()).isTrue();
	}

	@Test
	void rejectsBlankName() {
		assertThatThrownBy(() -> new Ability(" ", false)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new Ability(null, false)).isInstanceOf(DomainValidationException.class);
	}
}
