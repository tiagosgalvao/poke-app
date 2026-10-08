package com.poke.catalog.client;

import com.poke.catalog.client.dto.NamedResource;
import com.poke.catalog.client.dto.PokemonDto;
import com.poke.catalog.client.dto.SpeciesDto;
import com.poke.catalog.domain.PokemonCatalog;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PokeApiPokemonCatalog implements PokemonCatalog {

	private final PokeApiClient client;

	public PokeApiPokemonCatalog(PokeApiClient client) {
		this.client = client;
	}

	@Override
	public Page<PokemonSummary> findPage(PageRequest request) {
		var list = client.list(request.offset(), request.size());
		var summaries = list.results().stream()
				.map(entry -> summaryOf(entry.id()))
				.toList();
		return new Page<>(summaries, request.page(), request.size(), list.count());
	}

	@Override
	public Optional<PokemonDetail> findDetail(String key) {
		return client.pokemon(key).map(this::detailOf);
	}

	private PokemonSummary summaryOf(int id) {
		var pokemon = client.pokemon(Integer.toString(id))
				.orElseThrow(() -> new MalformedPokeApiResponseException(
						"PokeAPI listed Pokemon " + id + " but could not return it"));
		var species = speciesOf(pokemon);
		return PokeApiMapper.toSummary(pokemon, species.flatMap(PokeApiMapper::englishGenus).orElse(null));
	}

	private PokemonDetail detailOf(PokemonDto pokemon) {
		var species = speciesOf(pokemon);
		return PokeApiMapper.toDetail(
				pokemon,
				species.flatMap(PokeApiMapper::englishGenus).orElse(null),
				species.flatMap(PokeApiMapper::latestEnglishFlavorText).orElse(null),
				List.of());
	}

	// Always follows species.url: for alternate forms (id ≥ 10001) the species id differs.
	private Optional<SpeciesDto> speciesOf(PokemonDto pokemon) {
		return Optional.ofNullable(pokemon.species())
				.map(NamedResource::id)
				.flatMap(client::species);
	}
}
