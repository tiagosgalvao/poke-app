package com.poke.shared.measure;

public final class Measures {

	private static final double HECTOGRAMS_PER_KILOGRAM = 10.0;
	private static final double DECIMETRES_PER_METRE = 10.0;

	private Measures() {
	}

	public static double kilograms(int hectograms) {
		return hectograms / HECTOGRAMS_PER_KILOGRAM;
	}

	public static double metres(int decimetres) {
		return decimetres / DECIMETRES_PER_METRE;
	}
}
