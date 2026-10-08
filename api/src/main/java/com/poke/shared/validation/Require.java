package com.poke.shared.validation;

import com.poke.shared.exception.DomainValidationException;

import java.util.List;

public final class Require {

	private Require() {
	}

	public static String text(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new DomainValidationException(field + " must not be blank");
		}
		return value;
	}

	public static int positive(int value, String field) {
		if (value <= 0) {
			throw new DomainValidationException(field + " must be positive");
		}
		return value;
	}

	public static int nonNegative(int value, String field) {
		if (value < 0) {
			throw new DomainValidationException(field + " must not be negative");
		}
		return value;
	}

	public static long nonNegative(long value, String field) {
		if (value < 0) {
			throw new DomainValidationException(field + " must not be negative");
		}
		return value;
	}

	public static <T> T present(T value, String field) {
		if (value == null) {
			throw new DomainValidationException(field + " must be present");
		}
		return value;
	}

	public static <T> List<T> copy(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
