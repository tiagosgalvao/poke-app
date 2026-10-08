package com.poke.catalog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeciesDto(
		int id,
		String name,
		List<Genus> genera,
		@JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
		@JsonProperty("evolution_chain") ApiResource evolutionChain) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Genus(String genus, NamedResource language) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FlavorText(@JsonProperty("flavor_text") String text, NamedResource language, NamedResource version) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ApiResource(String url) {
	}
}
