package com.poke.catalog.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.poke.TestcontainersConfiguration;
import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.catalog.service.CatalogService;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.poke.catalog.client.PokeApiFixtures.stubFixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PokeApiCacheIntegrationTest {

	@RegisterExtension
	static WireMockExtension pokeApi = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@DynamicPropertySource
	static void pointToWireMock(DynamicPropertyRegistry registry) {
		registry.add("poke.pokeapi.base-url", pokeApi::baseUrl);
	}

	@Autowired
	CatalogService catalogService;

	@Autowired
	CacheManager cacheManager;

	@BeforeEach
	void clearCaches() {
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
	}

	@Test
	void repeatedDetailLookupsAreServedFromRedis() throws IOException {
		stubFixture(pokeApi, "/pokemon/25/", "pokemon-25.json");
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");
		stubFixture(pokeApi, "/evolution-chain/10/", "evolution-chain-10.json");

		var first = catalogService.getDetail("25");
		var second = catalogService.getDetail("25");

		assertThat(second).isEqualTo(first);
		assertThat(second.evolution()).hasSize(3);
		pokeApi.verify(1, getRequestedFor(urlEqualTo("/pokemon/25/")));
		pokeApi.verify(1, getRequestedFor(urlEqualTo("/pokemon-species/25/")));
		pokeApi.verify(1, getRequestedFor(urlEqualTo("/evolution-chain/10/")));
	}

	@Test
	void pagesReuseCachedEntries() throws IOException {
		stubFixture(pokeApi, "/pokemon?offset=24&limit=2", "pokemon-list-offset24-limit2.json");
		stubFixture(pokeApi, "/pokemon/25/", "pokemon-25.json");
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");
		stubFixture(pokeApi, "/pokemon/26/", "pokemon-26.json");
		stubFixture(pokeApi, "/pokemon-species/26/", "pokemon-species-26.json");

		catalogService.browse(new PageRequest(12, 2));
		var cached = catalogService.browse(new PageRequest(12, 2));

		assertThat(cached.content()).hasSize(2);
		pokeApi.verify(1, getRequestedFor(urlEqualTo("/pokemon?offset=24&limit=2")));
		pokeApi.verify(1, getRequestedFor(urlEqualTo("/pokemon/26/")));
	}

	@Test
	void notFoundResultsAreNotCached() {
		pokeApi.stubFor(get("/pokemon/missingno/").willReturn(aResponse().withStatus(404)));

		assertThatThrownBy(() -> catalogService.getDetail("missingno")).isInstanceOf(PokemonNotFoundException.class);
		assertThatThrownBy(() -> catalogService.getDetail("missingno")).isInstanceOf(PokemonNotFoundException.class);

		pokeApi.verify(2, getRequestedFor(urlEqualTo("/pokemon/missingno/")));
	}
}
