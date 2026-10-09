package com.poke.catalog.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class PokeApiFixtures {

	private static final String FIXTURE_FOLDER = "/pokeapi/";

	private PokeApiFixtures() {
	}

	public static String fixture(String name) throws IOException {
		try (var in = PokeApiFixtures.class.getResourceAsStream(FIXTURE_FOLDER + name)) {
			if (in == null) {
				throw new FileNotFoundException("missing fixture " + name);
			}
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	public static void stubFixture(WireMockExtension pokeApi, String url, String fixture) throws IOException {
		pokeApi.stubFor(get(urlEqualTo(url)).willReturn(okJson(fixture(fixture))));
	}

	public static void stubSlowFixture(WireMockExtension pokeApi, String url, String fixture, Duration delay) throws IOException {
		pokeApi.stubFor(get(urlEqualTo(url)).willReturn(okJson(fixture(fixture)).withFixedDelay((int) delay.toMillis())));
	}
}
