package com.poke.identity.domain;

import com.poke.shared.exception.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

	public EmailAlreadyRegisteredException() {
		super("This email is already registered");
	}
}
