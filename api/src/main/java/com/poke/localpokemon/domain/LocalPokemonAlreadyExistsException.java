package com.poke.localpokemon.domain;

import com.poke.shared.exception.ConflictException;

public class LocalPokemonAlreadyExistsException extends ConflictException {

	public LocalPokemonAlreadyExistsException(int id) {
		super("Pokemon " + id + " is already in the local Pokedex");
	}
}
