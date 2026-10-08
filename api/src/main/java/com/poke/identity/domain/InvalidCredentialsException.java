package com.poke.identity.domain;

import com.poke.shared.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}
}
