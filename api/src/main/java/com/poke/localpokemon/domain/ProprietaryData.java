package com.poke.localpokemon.domain;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

import java.util.*;

public record ProprietaryData(String localizedName, String region, String habitat, Set<String> tags, String notes) {

	public static final int MAX_TAGS = 10;
	public static final ProprietaryData NONE = new ProprietaryData(null, null, null, Set.of(), null);

	public ProprietaryData {
		tags = normalized(tags);
	}

	private static Set<String> normalized(Set<String> tags) {
		SortedSet<String> normalized = new TreeSet<>();
		if (tags != null) {
			tags.forEach(tag -> normalized.add(Require.text(tag, "tag").strip().toLowerCase(Locale.ROOT)));
		}
		if (normalized.size() > MAX_TAGS) {
			throw new DomainValidationException("a Pokemon can have at most " + MAX_TAGS + " tags");
		}
		return Collections.unmodifiableSortedSet(normalized);
	}
}
