package com.poke.identity.domain;

import com.poke.shared.exception.ConflictException;

public class UsernameAlreadyTakenException extends ConflictException {

	public UsernameAlreadyTakenException(String username) {
		super("Username '" + username + "' is already taken");
	}
}
