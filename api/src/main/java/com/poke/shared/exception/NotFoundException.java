package com.poke.shared.exception;

public abstract class NotFoundException extends DomainException {

	protected NotFoundException(String message) {
		super(message);
	}
}
