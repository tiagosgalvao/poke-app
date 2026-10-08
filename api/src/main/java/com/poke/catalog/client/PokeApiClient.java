package com.poke.catalog.client;

import com.poke.catalog.client.dto.EvolutionChainDto;
import com.poke.catalog.client.dto.PokemonDto;
import com.poke.catalog.client.dto.PokemonListDto;
import com.poke.catalog.client.dto.SpeciesDto;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Optional;

import static com.poke.catalog.client.PokeApiCacheNames.EVOLUTION_CHAIN;
import static com.poke.catalog.client.PokeApiCacheNames.POKEMON;
import static com.poke.catalog.client.PokeApiCacheNames.POKEMON_PAGE;
import static com.poke.catalog.client.PokeApiCacheNames.SPECIES;
import static com.poke.catalog.client.enums.PokeApiLanguage.ENGLISH;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static org.springframework.http.HttpHeaders.USER_AGENT;

@Component
public class PokeApiClient {

	private static final String CLIENT_NAME = "poke-app";
	private static final String POKEMON_LIST_URI = "/pokemon?offset={offset}&limit={limit}";
	private static final String POKEMON_URI = "/pokemon/{key}/";
	private static final String SPECIES_URI = "/pokemon-species/{id}/";
	private static final String EVOLUTION_CHAIN_URI = "/evolution-chain/{id}/";
	private static final String UNLESS_NOT_FOUND = "#result == null";

	private final RestClient http;

	public PokeApiClient(PokeApiProperties properties, RestClient.Builder restClientBuilder) {
		this.http = restClientBuilder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory(properties))
				.defaultHeader(USER_AGENT, CLIENT_NAME)
				.build();
	}

	private static JdkClientHttpRequestFactory requestFactory(PokeApiProperties properties) {
		var httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.followRedirects(NORMAL)
				.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return requestFactory;
	}

	@Cacheable(cacheNames = POKEMON_PAGE, key = "#offset + ':' + #limit")
	public PokemonListDto list(long offset, int limit) {
		return fetch(PokemonListDto.class, POKEMON_LIST_URI, offset, limit)
				.orElseThrow(() -> new MalformedPokeApiResponseException("PokeAPI returned no Pokemon list"));
	}

	@Cacheable(cacheNames = POKEMON, unless = UNLESS_NOT_FOUND)
	public Optional<PokemonDto> pokemon(String key) {
		return fetch(PokemonDto.class, POKEMON_URI, key);
	}

	@Cacheable(cacheNames = SPECIES, unless = UNLESS_NOT_FOUND)
	public Optional<SpeciesDto> species(int id) {
		return fetch(SpeciesDto.class, SPECIES_URI, id)
				.map(species -> species.keepingOnlyLatestTextIn(ENGLISH.code()));
	}

	@Cacheable(cacheNames = EVOLUTION_CHAIN, unless = UNLESS_NOT_FOUND)
	public Optional<EvolutionChainDto> evolutionChain(int id) {
		return fetch(EvolutionChainDto.class, EVOLUTION_CHAIN_URI, id);
	}

	private <T> Optional<T> fetch(Class<T> type, String uri, Object... variables) {
		try {
			return Optional.ofNullable(http.get().uri(uri, variables).retrieve().body(type));
		}
		catch (HttpClientErrorException.NotFound notFound) {
			return Optional.empty();
		}
		catch (RestClientException failure) {
			throw new ExternalServiceUnavailableException("PokeAPI request failed: " + uri, failure);
		}
	}
}
