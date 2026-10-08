package com.poke.catalog.domain;

import com.poke.shared.exception.NotFoundException;

public class PokemonNotFoundException extends NotFoundException {

	public PokemonNotFoundException(String idOrName) {
		super("Pokemon '" + idOrName + "' was not found");
	}
}
