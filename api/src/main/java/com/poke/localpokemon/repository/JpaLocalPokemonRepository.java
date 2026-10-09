package com.poke.localpokemon.repository;

import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.LocalPokemonRepository;
import com.poke.localpokemon.domain.StaleVersionException;
import com.poke.localpokemon.entity.LocalPokemonEntity;
import com.poke.localpokemon.entity.LocalPokemonEntityMapper;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

	private static final Sort BY_NATIONAL_ID = Sort.by("id");

	private final LocalPokemonJpaRepository jpa;
	private final LocalPokemonEntityMapper mapper;

	public JpaLocalPokemonRepository(LocalPokemonJpaRepository jpa, LocalPokemonEntityMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Optional<LocalPokemon> findById(int id) {
		return jpa.findById(id).map(mapper::toDomain);
	}

	@Override
	public boolean existsById(int id) {
		return jpa.existsById(id);
	}

	@Override
	public Page<LocalPokemon> findPage(PageRequest request) {
		var page = jpa.findAll(org.springframework.data.domain.PageRequest.of(request.page(), request.size(), BY_NATIONAL_ID));
		return new Page<>(page.map(mapper::toDomain).getContent(), request.page(), request.size(),
			page.getTotalElements());
	}

	@Override
	@Transactional
	public LocalPokemon save(LocalPokemon pokemon) {
		var entity = jpa.findById(pokemon.id()).orElseGet(() -> new LocalPokemonEntity(pokemon.id()));
		entity.copyFrom(pokemon);
		try {
			return mapper.toDomain(jpa.saveAndFlush(entity));
		} catch (ObjectOptimisticLockingFailureException concurrentUpdate) {
			throw new StaleVersionException(pokemon.id());
		}
	}

	@Override
	@Transactional
	public void deleteById(int id) {
		jpa.deleteById(id);
	}
}
