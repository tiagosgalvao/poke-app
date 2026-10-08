package com.poke.catalog.service;

import com.poke.catalog.domain.PokemonCatalog;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonKey;
import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

	private final PokemonCatalog catalog;

	public CatalogService(PokemonCatalog catalog) {
		this.catalog = catalog;
	}

	public Page<PokemonSummary> browse(PageRequest request) {
		if (request == null) {
			throw new DomainValidationException("page request must not be null");
		}
		return catalog.findPage(request);
	}

	public PokemonDetail getDetail(String idOrName) {
		var key = PokemonKey.parse(idOrName);
		return catalog.findDetail(key.value())
				.orElseThrow(() -> new PokemonNotFoundException(key.value()));
	}
}
