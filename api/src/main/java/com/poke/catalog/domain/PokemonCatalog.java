package com.poke.catalog.domain;

import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;

import java.util.Optional;

public interface PokemonCatalog {

	Page<PokemonSummary> findPage(PageRequest request);

	Optional<PokemonDetail> findDetail(String key);
}
