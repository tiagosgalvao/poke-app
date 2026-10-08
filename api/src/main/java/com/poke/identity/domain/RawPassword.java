package com.poke.identity.domain;

import com.poke.shared.exception.DomainValidationException;

public record RawPassword(String value) {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 72;

	private static final String MASKED = "RawPassword[******]";

	public RawPassword {
		if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
			throw new DomainValidationException(
					"password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
		}
	}

	@Override
	public String toString() {
		return MASKED;
	}
}
