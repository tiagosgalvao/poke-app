package com.poke.shared.pagination;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

public record PageRequest(int page, int size) {

	public static final int MAX_SIZE = 50;

	public PageRequest {
		Require.nonNegative(page, "page");
		if (size < 1 || size > MAX_SIZE) {
			throw new DomainValidationException("size must be between 1 and " + MAX_SIZE);
		}
	}

	public long offset() {
		return (long) page * size;
	}
}
