package com.poke.catalog.client.dto;

import java.util.List;

public final class NullSafeLists {

	private NullSafeLists() {
	}

	public static <T> List<T> orEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}
}
