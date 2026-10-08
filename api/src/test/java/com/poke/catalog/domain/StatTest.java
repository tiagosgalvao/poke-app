package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatTest {

	@Test
	void keepsNameAndValue() {
		var stat = new Stat("speed", 90);

		assertThat(stat.name()).isEqualTo("speed");
		assertThat(stat.value()).isEqualTo(90);
	}

	@Test
	void rejectsBlankNameAndNegativeValue() {
		assertThatThrownBy(() -> new Stat("", 10)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new Stat("hp", -1)).isInstanceOf(DomainValidationException.class);
	}
}
