package com.poke.catalog.domain;

import com.poke.shared.validation.Require;

public record Ability(String name, boolean hidden) {

	public Ability {
		Require.text(name, "ability name");
	}
}
