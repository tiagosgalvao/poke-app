package com.poke.shared.pagination;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

	public Page {
		if (content == null) {
			throw new DomainValidationException("content must not be null");
		}
		content = List.copyOf(content);
		Require.nonNegative(page, "page");
		Require.positive(size, "size");
		Require.nonNegative(totalElements, "totalElements");
	}

	public int totalPages() {
		return (int) ((totalElements + size - 1) / size);
	}

	public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
		return new Page<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
	}
}
