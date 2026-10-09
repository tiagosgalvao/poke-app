package com.poke.catalog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
	int id,
	String name,
	int height,
	int weight,
	List<AbilitySlot> abilities,
	List<TypeSlot> types,
	List<StatValue> stats,
	Sprites sprites,
	NamedResource species) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AbilitySlot(@JsonProperty("is_hidden") boolean hidden, int slot, NamedResource ability) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TypeSlot(int slot, NamedResource type) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record StatValue(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Artwork(@JsonProperty("front_default") String frontDefault) {
	}
}
