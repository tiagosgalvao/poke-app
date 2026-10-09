package com.poke.localpokemon.entity;

import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.UpstreamData;
import com.poke.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface LocalPokemonEntityMapper {

	@Mapping(target = "upstream", source = ".")
	@Mapping(target = "proprietary", source = ".")
	LocalPokemon toDomain(LocalPokemonEntity entity);

	UpstreamData toUpstream(LocalPokemonEntity entity);

	ProprietaryData toProprietary(LocalPokemonEntity entity);
}
