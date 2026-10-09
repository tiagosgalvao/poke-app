package com.poke.localpokemon.domain;

import com.poke.shared.exception.ConflictException;

public class StaleVersionException extends ConflictException {

	public StaleVersionException(int id, long expectedVersion, long currentVersion) {
		super("Local Pokemon " + id + " was modified meanwhile (version " + currentVersion + ", not "
			+ expectedVersion + "). Reload it and try again.");
	}

	public StaleVersionException(int id) {
		super("Local Pokemon " + id + " was modified meanwhile. Reload it and try again.");
	}
}
