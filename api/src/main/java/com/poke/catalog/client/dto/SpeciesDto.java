package com.poke.catalog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.poke.catalog.client.dto.NamedResource.isNamed;
import static com.poke.catalog.client.dto.NullSafeLists.orEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeciesDto(
		int id,
		String name,
		List<Genus> genera,
		@JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
		@JsonProperty("evolution_chain") ApiResource evolutionChain) {

	public SpeciesDto keepingOnlyLatestTextIn(String language) {
		var genusInLanguage = orEmpty(genera).stream()
				.filter(genus -> isNamed(genus.language(), language))
				.limit(1)
				.toList();
		var latestFlavorTextInLanguage = orEmpty(flavorTextEntries).stream()
				.filter(entry -> isNamed(entry.language(), language))
				.reduce((first, second) -> second)
				.map(List::of)
				.orElse(List.of());
		return new SpeciesDto(id, name, genusInLanguage, latestFlavorTextInLanguage, evolutionChain);
	}

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
