package com.poke.catalog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedResource(String name, String url) {

	public int id() {
		return ResourceUrls.idOf(url);
	}

	public static boolean isNamed(NamedResource resource, String expectedName) {
		return resource != null && expectedName.equals(resource.name());
	}
}
