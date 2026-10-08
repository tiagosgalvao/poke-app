package com.poke.localpokemon.service;

import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.service.CatalogService;
import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.LocalPokemonAlreadyExistsException;
import com.poke.localpokemon.domain.LocalPokemonNotFoundException;
import com.poke.localpokemon.domain.LocalPokemonRepository;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.ProprietaryPatch;
import com.poke.localpokemon.domain.UpstreamData;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class LocalPokemonService {

	private final LocalPokemonRepository repository;
	private final CatalogService catalogService;
	private final Clock clock;

	public LocalPokemonService(LocalPokemonRepository repository, CatalogService catalogService, Clock clock) {
		this.repository = repository;
		this.catalogService = catalogService;
		this.clock = clock;
	}

	@Transactional
	public LocalPokemon importPokemon(String idOrName) {
		var detail = catalogService.getDetail(idOrName);
		if (repository.existsById(detail.id())) {
			throw new LocalPokemonAlreadyExistsException(detail.id());
		}
		return repository.save(LocalPokemon.importFrom(detail.id(), upstreamOf(detail), clock.instant()));
	}

	@Transactional(readOnly = true)
	public LocalPokemon get(int id) {
		return repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<LocalPokemon> list(PageRequest request) {
		return repository.findPage(request);
	}

	@Transactional
	public LocalPokemon update(int id, long expectedVersion, ProprietaryData data) {
		var current = get(id);
		current.checkVersion(expectedVersion);
		return repository.save(current.withProprietary(data, clock.instant()));
	}

	@Transactional
	public LocalPokemon patch(int id, long expectedVersion, ProprietaryPatch patch) {
		var current = get(id);
		current.checkVersion(expectedVersion);
		return repository.save(current.withProprietary(patch.applyTo(current.proprietary()), clock.instant()));
	}

	@Transactional
	public void delete(int id) {
		if (!repository.existsById(id)) {
			throw new LocalPokemonNotFoundException(id);
		}
		repository.deleteById(id);
	}

	static UpstreamData upstreamOf(PokemonDetail detail) {
		return new UpstreamData(
				detail.name(),
				detail.spriteUrl(),
				detail.imageUrl(),
				detail.category(),
				detail.weightHectograms(),
				detail.heightDecimetres(),
				detail.types(),
				detail.abilities().stream().map(Ability::name).toList());
	}
}
