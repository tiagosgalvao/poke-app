package com.poke.catalog.client.enums;

public enum PokeApiLanguage {

	ENGLISH("en");

	private final String code;

	PokeApiLanguage(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}
