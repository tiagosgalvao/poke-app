package com.poke.catalog.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.catalog.domain.Stat;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.poke.catalog.client.PokeApiFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiPokemonCatalogTest {

	private static final Duration READ_TIMEOUT = Duration.ofMillis(800);
	private static final Duration SLOW_RESPONSE = Duration.ofMillis(400);

	@RegisterExtension
	static WireMockExtension pokeApi = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	private PokeApiPokemonCatalog catalog;

	@BeforeEach
	void setUp() {
		var properties = new PokeApiProperties(pokeApi.baseUrl(), Duration.ofSeconds(1), READ_TIMEOUT);
		catalog = new PokeApiPokemonCatalog(new PokeApiClient(properties, RestClient.builder()));
	}

	@Test
	void findPageMapsEachListedPokemonWithItsSpecies() throws IOException {
		stubFixture(pokeApi, "/pokemon?offset=24&limit=2", "pokemon-list-offset24-limit2.json");
		stubFixture(pokeApi, "/pokemon/25/", "pokemon-25.json");
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");
		stubFixture(pokeApi, "/pokemon/26/", "pokemon-26.json");
		stubFixture(pokeApi, "/pokemon-species/26/", "pokemon-species-26.json");

		var page = catalog.findPage(new PageRequest(12, 2));

		assertThat(page.totalElements()).isEqualTo(1351);
		assertThat(page.totalPages()).isEqualTo(676);
		assertThat(page.page()).isEqualTo(12);
		assertThat(page.content()).hasSize(2);

		var pikachu = page.content().getFirst();
		assertThat(pikachu.id()).isEqualTo(25);
		assertThat(pikachu.name()).isEqualTo("pikachu");
		assertThat(pikachu.spriteUrl()).endsWith("/sprites/pokemon/25.png");
		assertThat(pikachu.category()).isEqualTo("Mouse Pokémon");
		assertThat(pikachu.weightHectograms()).isEqualTo(60);
		assertThat(pikachu.heightDecimetres()).isEqualTo(4);
		assertThat(pikachu.types()).containsExactly("electric");
		assertThat(pikachu.abilities()).containsExactly(new Ability("static", false), new Ability("lightning-rod", true));

		assertThat(page.content().get(1).name()).isEqualTo("raichu");
	}

	@Test
	void findPageFetchesEntriesConcurrently() throws IOException {
		stubFixture(pokeApi, "/pokemon?offset=24&limit=2", "pokemon-list-offset24-limit2.json");
		stubSlowFixture(pokeApi, "/pokemon/25/", "pokemon-25.json", SLOW_RESPONSE);
		stubSlowFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json", SLOW_RESPONSE);
		stubSlowFixture(pokeApi, "/pokemon/26/", "pokemon-26.json", SLOW_RESPONSE);
		stubSlowFixture(pokeApi, "/pokemon-species/26/", "pokemon-species-26.json", SLOW_RESPONSE);

		long started = System.nanoTime();
		var page = catalog.findPage(new PageRequest(12, 2));
		var elapsed = Duration.ofNanos(System.nanoTime() - started);

		var sequentialTime = SLOW_RESPONSE.multipliedBy(4);
		assertThat(page.content()).hasSize(2);
		assertThat(elapsed).isLessThan(sequentialTime.minus(SLOW_RESPONSE));
	}

	@Test
	void findPageKeepsTheUpstreamOrderEvenWhenEarlierEntriesAreSlower() throws IOException {
		stubFixture(pokeApi, "/pokemon?offset=24&limit=2", "pokemon-list-offset24-limit2.json");
		stubSlowFixture(pokeApi, "/pokemon/25/", "pokemon-25.json", SLOW_RESPONSE);
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");
		stubFixture(pokeApi, "/pokemon/26/", "pokemon-26.json");
		stubFixture(pokeApi, "/pokemon-species/26/", "pokemon-species-26.json");

		var page = catalog.findPage(new PageRequest(12, 2));

		assertThat(page.content()).extracting(PokemonSummary::name).containsExactly("pikachu", "raichu");
	}

	@Test
	void findDetailMapsArtworkStatsAndLatestEnglishDescription() throws IOException {
		stubFixture(pokeApi, "/pokemon/pikachu/", "pokemon-25.json");
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");

		var detail = catalog.findDetail("pikachu").orElseThrow();

		assertThat(detail.spriteUrl()).endsWith("/sprites/pokemon/25.png");
		assertThat(detail.imageUrl()).endsWith("/official-artwork/25.png");
		assertThat(detail.stats()).hasSize(6).contains(new Stat("hp", 35), new Stat("speed", 90));
		assertThat(detail.description()).startsWith("Possesses cheek sacs");
		assertThat(detail.category()).isEqualTo("Mouse Pokémon");
	}

	@Test
	void findDetailIncludesTheFlattenedEvolutionChain() throws IOException {
		stubFixture(pokeApi, "/pokemon/133/", "pokemon-133.json");
		stubFixture(pokeApi, "/pokemon-species/133/", "pokemon-species-133.json");
		stubFixture(pokeApi, "/evolution-chain/67/", "evolution-chain-67.json");

		var eevee = catalog.findDetail("133").orElseThrow();

		assertThat(eevee.evolution()).hasSize(9);
		assertThat(eevee.evolution().getFirst().name()).isEqualTo("eevee");
		assertThat(eevee.description()).doesNotContain("\n", "\f");
	}

	@Test
	void missingEvolutionChainLeavesTheEvolutionEmpty() throws IOException {
		stubFixture(pokeApi, "/pokemon/25/", "pokemon-25.json");
		stubFixture(pokeApi, "/pokemon-species/25/", "pokemon-species-25.json");
		pokeApi.stubFor(get("/evolution-chain/10/").willReturn(aResponse().withStatus(404)));

		assertThat(catalog.findDetail("25").orElseThrow().evolution()).isEmpty();
	}

	@Test
	void findDetailFollowsTheSpeciesUrlForAlternateForms() throws IOException {
		stubFixture(pokeApi, "/pokemon/10001/", "pokemon-10001.json");
		stubFixture(pokeApi, "/pokemon-species/386/", "pokemon-species-386.json");

		var deoxysAttack = catalog.findDetail("10001").orElseThrow();

		assertThat(deoxysAttack.name()).isEqualTo("deoxys-attack");
		assertThat(deoxysAttack.category()).isEqualTo("DNA Pokémon");
		pokeApi.verify(getRequestedFor(urlEqualTo("/pokemon-species/386/")));
	}

	@Test
	void findDetailIsEmptyWhenPokeApiHasNoSuchPokemon() {
		pokeApi.stubFor(get("/pokemon/missingno/")
			.willReturn(aResponse().withStatus(404).withBody("{\"status\":404,\"message\":\"Not Found\"}")));

		assertThat(catalog.findDetail("missingno")).isEmpty();
	}

	@Test
	void missingSpeciesLeavesTheCategoryEmpty() throws IOException {
		stubFixture(pokeApi, "/pokemon/25/", "pokemon-25.json");
		pokeApi.stubFor(get("/pokemon-species/25/").willReturn(aResponse().withStatus(404)));

		var detail = catalog.findDetail("25").orElseThrow();

		assertThat(detail.category()).isNull();
		assertThat(detail.description()).isNull();
	}

	@Test
	void serverErrorsBecomeUnavailable() {
		pokeApi.stubFor(get("/pokemon/25/").willReturn(serverError()));

		assertThatThrownBy(() -> catalog.findDetail("25")).isInstanceOf(ExternalServiceUnavailableException.class);
	}

	@Test
	void slowResponsesTimeOutAsUnavailable() throws IOException {
		pokeApi.stubFor(get("/pokemon/25/").willReturn(okJson(fixture("pokemon-25.json"))
			.withFixedDelay((int) READ_TIMEOUT.multipliedBy(3).toMillis())));

		assertThatThrownBy(() -> catalog.findDetail("25")).isInstanceOf(ExternalServiceUnavailableException.class);
	}

	@Test
	void listedPokemonThatCannotBeFetchedFailsThePage() throws IOException {
		stubFixture(pokeApi, "/pokemon?offset=24&limit=2", "pokemon-list-offset24-limit2.json");
		pokeApi.stubFor(get("/pokemon/25/").willReturn(aResponse().withStatus(404)));

		assertThatThrownBy(() -> catalog.findPage(new PageRequest(12, 2)))
			.isInstanceOf(ExternalServiceUnavailableException.class);
	}
}
