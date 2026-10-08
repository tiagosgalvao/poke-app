package com.poke.catalog.domain;

import com.poke.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PokemonNotFoundExceptionTest {

	@Test
	void isANotFoundErrorNamingTheLookupKey() {
		var ex = new PokemonNotFoundException("missingno");

		assertThat(ex).isInstanceOf(NotFoundException.class);
		assertThat(ex.getMessage()).isEqualTo("Pokemon 'missingno' was not found");
	}
}
