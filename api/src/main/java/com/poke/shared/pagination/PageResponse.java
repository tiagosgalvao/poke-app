package com.poke.shared.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <D, T> PageResponse<T> from(Page<D> page, Function<? super D, ? extends T> toResponse) {
		return new PageResponse<>(
				page.content().stream().<T>map(toResponse).toList(),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages());
	}
}
