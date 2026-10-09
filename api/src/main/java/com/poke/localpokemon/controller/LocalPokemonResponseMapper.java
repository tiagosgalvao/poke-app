package com.poke.localpokemon.controller;

import com.poke.localpokemon.controller.LocalPokemonResponses.LocalPokemonResponse;
import com.poke.localpokemon.controller.LocalPokemonResponses.SyncSummaryResponse;
import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.SyncSummary;
import com.poke.shared.mapping.MappingConfig;
import com.poke.shared.mapping.MeasureMappings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static com.poke.shared.mapping.MeasureMappings.KILOGRAMS;
import static com.poke.shared.mapping.MeasureMappings.METRES;

@Mapper(config = MappingConfig.class, uses = MeasureMappings.class)
interface LocalPokemonResponseMapper {

	@Mapping(target = ".", source = "upstream")
	@Mapping(target = ".", source = "proprietary")
	@Mapping(target = "weightKg", source = "upstream.weightHectograms", qualifiedByName = KILOGRAMS)
	@Mapping(target = "heightM", source = "upstream.heightDecimetres", qualifiedByName = METRES)
	LocalPokemonResponse toResponse(LocalPokemon pokemon);

	SyncSummaryResponse toSyncResponse(SyncSummary summary);
}
