package com.poke.localpokemon.domain;

import java.util.List;

public record SyncSummary(List<Integer> created, List<Integer> refreshed, List<Integer> failed) {

	public SyncSummary {
		created = List.copyOf(created);
		refreshed = List.copyOf(refreshed);
		failed = List.copyOf(failed);
	}
}
