package com.poke.shared.mapping;

import com.poke.shared.measure.Measures;
import org.mapstruct.Named;

public final class MeasureMappings {

	public static final String KILOGRAMS = "kilograms";
	public static final String METRES = "metres";

	private MeasureMappings() {
	}

	@Named(KILOGRAMS)
	public static double kilograms(int hectograms) {
		return Measures.kilograms(hectograms);
	}

	@Named(METRES)
	public static double metres(int decimetres) {
		return Measures.metres(decimetres);
	}
}
