package com.poke.catalog.controller;

import com.poke.catalog.controller.PokemonResponses.PokemonDetailResponse;
import com.poke.catalog.controller.PokemonResponses.PokemonSummaryResponse;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.shared.mapping.MappingConfig;
import com.poke.shared.mapping.MeasureMappings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static com.poke.shared.mapping.MeasureMappings.KILOGRAMS;
import static com.poke.shared.mapping.MeasureMappings.METRES;

@Mapper(config = MappingConfig.class, uses = MeasureMappings.class)
interface PokemonResponseMapper {

	@Mapping(target = "weightKg", source = "weightHectograms", qualifiedByName = KILOGRAMS)
	@Mapping(target = "heightM", source = "heightDecimetres", qualifiedByName = METRES)
	PokemonSummaryResponse toSummaryResponse(PokemonSummary pokemon);

	@Mapping(target = "weightKg", source = "weightHectograms", qualifiedByName = KILOGRAMS)
	@Mapping(target = "heightM", source = "heightDecimetres", qualifiedByName = METRES)
	PokemonDetailResponse toDetailResponse(PokemonDetail pokemon);
}
