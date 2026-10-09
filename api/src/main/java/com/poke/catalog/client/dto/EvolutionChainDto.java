package com.poke.catalog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainDto(int id, ChainLink chain) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChainLink(
		NamedResource species,
		@JsonProperty("evolution_details") List<EvolutionDetail> evolutionDetails,
		@JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EvolutionDetail(
		NamedResource trigger,
		NamedResource item,
		@JsonProperty("min_level") Integer minLevel) {
	}
}
