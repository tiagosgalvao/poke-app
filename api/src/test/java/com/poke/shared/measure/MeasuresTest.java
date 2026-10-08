package com.poke.shared.measure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeasuresTest {

	@Test
	void convertsPokeApiUnitsToMetricUnits() {
		assertThat(Measures.kilograms(60)).isEqualTo(6.0);
		assertThat(Measures.metres(4)).isEqualTo(0.4);
	}
}
