package com.poke.localpokemon.domain;

import com.poke.shared.validation.Require;

import java.time.Instant;

public record LocalPokemon(
	int id,
	UpstreamData upstream,
	ProprietaryData proprietary,
	long version,
	Instant syncedAt,
	Instant updatedAt) {

	private static final long INITIAL_VERSION = 0;

	public LocalPokemon {
		Require.positive(id, "id");
		Require.present(upstream, "upstream data");
		Require.present(proprietary, "proprietary data");
		Require.nonNegative(version, "version");
		Require.present(syncedAt, "synced at");
		Require.present(updatedAt, "updated at");
	}

	public static LocalPokemon importFrom(int id, UpstreamData upstream, Instant now) {
		return new LocalPokemon(id, upstream, ProprietaryData.NONE, INITIAL_VERSION, now, now);
	}

	public LocalPokemon refreshedWith(UpstreamData fresh, Instant now) {
		return new LocalPokemon(id, fresh, proprietary, version, now, now);
	}

	public LocalPokemon withProprietary(ProprietaryData data, Instant now) {
		return new LocalPokemon(id, upstream, data, version, syncedAt, now);
	}

	public void checkVersion(long expectedVersion) {
		if (expectedVersion != version) {
			throw new StaleVersionException(id, expectedVersion, version);
		}
	}
}
