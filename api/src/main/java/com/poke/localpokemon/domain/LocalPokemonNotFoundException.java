package com.poke.localpokemon.domain;

import com.poke.shared.exception.NotFoundException;

public class LocalPokemonNotFoundException extends NotFoundException {

	public LocalPokemonNotFoundException(int id) {
		super("Local Pokemon " + id + " was not found");
	}
}
