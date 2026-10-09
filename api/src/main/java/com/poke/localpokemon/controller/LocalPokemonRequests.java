package com.poke.localpokemon.controller;

import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.ProprietaryPatch;
import com.poke.localpokemon.domain.SyncBatch;
import com.poke.shared.exception.DomainValidationException;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Set;

import static com.poke.localpokemon.domain.ProprietaryData.MAX_TAGS;
import static com.poke.localpokemon.domain.SyncBatch.MAX_IDS;

final class LocalPokemonRequests {

	static final int MAX_KEY_LENGTH = 50;
	static final int MAX_LOCALIZED_NAME_LENGTH = 100;
	static final int MAX_PLACE_LENGTH = 50;
	static final int MAX_TAG_LENGTH = 30;
	static final int MAX_NOTES_LENGTH = 1000;
	static final String NOT_BLANK_WHEN_PRESENT = "(?s).*\\S.*";
	static final String NOT_BLANK_MESSAGE = "must not be blank";
	static final String TAG_PATTERN = "[A-Za-z0-9-]+";
	static final String TAG_MESSAGE = "must contain only letters, digits and hyphens";

	private LocalPokemonRequests() {
	}

	record ImportRequest(@NotBlank @Size(max = MAX_KEY_LENGTH) String idOrName) {
	}

	record ProprietaryUpdateRequest(
		@NotNull @PositiveOrZero Long version,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_LOCALIZED_NAME_LENGTH) String localizedName,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_PLACE_LENGTH) String region,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_PLACE_LENGTH) String habitat,
		@Size(max = MAX_TAGS) Set<@NotBlank @Size(max = MAX_TAG_LENGTH) @Pattern(regexp = TAG_PATTERN, message = TAG_MESSAGE) String> tags,
		@Size(max = MAX_NOTES_LENGTH) String notes) {

		ProprietaryData toProprietaryData() {
			return new ProprietaryData(localizedName, region, habitat, tags, notes);
		}
	}

	record ProprietaryPatchRequest(
		@NotNull @PositiveOrZero Long version,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_LOCALIZED_NAME_LENGTH) String localizedName,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_PLACE_LENGTH) String region,
		@Pattern(regexp = NOT_BLANK_WHEN_PRESENT, message = NOT_BLANK_MESSAGE) @Size(max = MAX_PLACE_LENGTH) String habitat,
		@Size(max = MAX_TAGS) Set<@NotBlank @Size(max = MAX_TAG_LENGTH) @Pattern(regexp = TAG_PATTERN, message = TAG_MESSAGE) String> tags,
		@Size(max = MAX_NOTES_LENGTH) String notes) {

		ProprietaryPatch toPatch() {
			return new ProprietaryPatch(localizedName, region, habitat, tags, notes);
		}
	}

	record SyncRequest(@Size(max = MAX_IDS) List<@NotNull @Positive Integer> ids, @Positive Integer fromId,
	                   @Positive Integer toId) {

		SyncBatch toBatch() {
			if (ids != null) {
				return new SyncBatch(ids);
			}
			if (fromId != null && toId != null) {
				return SyncBatch.range(fromId, toId);
			}
			throw new DomainValidationException("provide either ids or both fromId and toId");
		}
	}
}
