package com.poke.shared.exception;

public abstract class UnauthorizedException extends DomainException {

	protected UnauthorizedException(String message) {
		super(message);
	}
}
