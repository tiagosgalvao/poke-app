package com.poke.localpokemon.repository;

import com.poke.localpokemon.entity.LocalPokemonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Integer> {
}
