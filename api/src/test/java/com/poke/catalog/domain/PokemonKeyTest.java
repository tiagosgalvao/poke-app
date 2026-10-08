package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonKeyTest {

	@ParameterizedTest
	@CsvSource({
			"25, 25",
			"025, 25",
			"' 133 ', 133",
			"Pikachu, pikachu",
			"'  MR-MIME ', mr-mime",
			"porygon-z, porygon-z",
			"deoxys-attack, deoxys-attack"
	})
	void normalizesIdsAndNames(String raw, String expected) {
		assertThat(PokemonKey.parse(raw).value()).isEqualTo(expected);
	}

	@Test
	void tellsIdsFromNames() {
		assertThat(PokemonKey.parse("25").isId()).isTrue();
		assertThat(PokemonKey.parse("pikachu").isId()).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "0", "000", "-1", "mr. mime", "pika chu", "pikachu!", "../etc", "%20"})
	void rejectsMalformedKeys(String raw) {
		assertThatThrownBy(() -> PokemonKey.parse(raw)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void rejectsOverlongKeys() {
		assertThatThrownBy(() -> PokemonKey.parse("a".repeat(51))).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> PokemonKey.parse("9".repeat(10))).isInstanceOf(DomainValidationException.class);
	}
}
