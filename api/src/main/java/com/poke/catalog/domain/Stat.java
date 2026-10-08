package com.poke.catalog.domain;

import com.poke.shared.validation.Require;

public record Stat(String name, int value) {

	public Stat {
		Require.text(name, "stat name");
		Require.nonNegative(value, "stat value");
	}
}
