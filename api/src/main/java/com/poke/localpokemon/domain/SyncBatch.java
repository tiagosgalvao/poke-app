package com.poke.localpokemon.domain;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

public record SyncBatch(List<Integer> ids) {

	public static final int MAX_IDS = 50;

	private static final String BATCH_SIZE_MESSAGE = "a sync batch must have between 1 and " + MAX_IDS + " ids";

	public SyncBatch {
		Require.present(ids, "ids");
		var distinct = new LinkedHashSet<Integer>();
		ids.forEach(id -> distinct.add(Require.positive(Require.present(id, "id"), "id")));
		if (distinct.isEmpty() || distinct.size() > MAX_IDS) {
			throw new DomainValidationException(BATCH_SIZE_MESSAGE);
		}
		ids = List.copyOf(distinct);
	}

	public static SyncBatch range(int fromId, int toId) {
		if (fromId > toId) {
			throw new DomainValidationException("fromId must not be greater than toId");
		}
		if ((long) toId - fromId + 1 > MAX_IDS) {
			throw new DomainValidationException(BATCH_SIZE_MESSAGE);
		}
		return new SyncBatch(IntStream.rangeClosed(fromId, toId).boxed().toList());
	}
}
