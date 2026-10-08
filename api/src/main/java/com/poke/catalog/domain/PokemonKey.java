package com.poke.catalog.domain;

import com.poke.shared.exception.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

public record PokemonKey(String value, boolean isId) {

	private static final Pattern DIGITS = Pattern.compile("\\d+");
	private static final int MAX_ID_DIGITS = 9;
	private static final Pattern NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
	private static final int MAX_NAME_LENGTH = 50;

	public static PokemonKey parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new DomainValidationException("Pokemon id or name must not be blank");
		}
		var candidate = raw.strip().toLowerCase(Locale.ROOT);
		if (DIGITS.matcher(candidate).matches()) {
			if (candidate.length() > MAX_ID_DIGITS) {
				throw new DomainValidationException("Pokemon id is too large");
			}
			int id = Integer.parseInt(candidate);
			if (id <= 0) {
				throw new DomainValidationException("Pokemon id must be positive");
			}
			return new PokemonKey(Integer.toString(id), true);
		}
		if (candidate.length() > MAX_NAME_LENGTH || !NAME.matcher(candidate).matches()) {
			throw new DomainValidationException(
					"Pokemon id or name must be a positive number or contain only letters, digits and hyphens");
		}
		return new PokemonKey(candidate, false);
	}
}
