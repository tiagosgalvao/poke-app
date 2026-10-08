package com.poke.catalog.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;

final class PokeApiFixtures {

	private static final String FIXTURE_FOLDER = "/pokeapi/";

	private PokeApiFixtures() {
	}

	static String fixture(String name) throws IOException {
		try (var in = PokeApiFixtures.class.getResourceAsStream(FIXTURE_FOLDER + name)) {
			if (in == null) {
				throw new FileNotFoundException("missing fixture " + name);
			}
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	static void stubFixture(WireMockExtension pokeApi, String url, String fixture) throws IOException {
		pokeApi.stubFor(get(urlEqualTo(url)).willReturn(okJson(fixture(fixture))));
	}

	static void stubSlowFixture(WireMockExtension pokeApi, String url, String fixture, Duration delay) throws IOException {
		pokeApi.stubFor(get(urlEqualTo(url)).willReturn(okJson(fixture(fixture)).withFixedDelay((int) delay.toMillis())));
	}
}
