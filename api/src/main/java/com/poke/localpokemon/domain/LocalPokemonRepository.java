package com.poke.localpokemon.domain;

import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;

import java.util.Optional;

public interface LocalPokemonRepository {

	Optional<LocalPokemon> findById(int id);

	boolean existsById(int id);

	Page<LocalPokemon> findPage(PageRequest request);

	LocalPokemon save(LocalPokemon pokemon);

	void deleteById(int id);
}
