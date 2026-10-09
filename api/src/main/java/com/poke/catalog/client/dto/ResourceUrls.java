package com.poke.catalog.client.dto;

import com.poke.catalog.client.MalformedPokeApiResponseException;

public final class ResourceUrls {

	private static final char PATH_SEPARATOR = '/';

	private ResourceUrls() {
	}

	public static int idOf(String url) {
		if (url == null) {
			throw new MalformedPokeApiResponseException("PokeAPI resource url is missing");
		}
		var withoutTrailingSlash = url.endsWith(String.valueOf(PATH_SEPARATOR)) ? url.substring(0, url.length() - 1) : url;
		var lastSegment = withoutTrailingSlash.substring(withoutTrailingSlash.lastIndexOf(PATH_SEPARATOR) + 1);
		try {
			return Integer.parseInt(lastSegment);
		} catch (NumberFormatException notANumber) {
			throw new MalformedPokeApiResponseException("PokeAPI resource url has no numeric id: " + url, notANumber);
		}
	}
}
