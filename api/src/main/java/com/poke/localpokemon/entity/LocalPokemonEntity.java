package com.poke.localpokemon.entity;

import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.UpstreamData;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "local_pokemon")
public class LocalPokemonEntity implements Persistable<Integer> {

	private static final String POKEMON_ID = "pokemon_id";
	private static final String POSITION = "position";
	private static final int PAGE_BATCH_SIZE = 50;

	@Id
	private Integer id;

	private String name;
	private String spriteUrl;
	private String imageUrl;
	private String category;
	private int weightHectograms;
	private int heightDecimetres;
	private String localizedName;
	private String region;
	private String habitat;
	private String notes;

	@Version
	private long version;

	private Instant syncedAt;
	private Instant updatedAt;

	@ElementCollection
	@CollectionTable(name = "local_pokemon_type", joinColumns = @JoinColumn(name = POKEMON_ID))
	@OrderColumn(name = POSITION)
	@Column(name = "type")
	@BatchSize(size = PAGE_BATCH_SIZE)
	private List<String> types = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "local_pokemon_ability", joinColumns = @JoinColumn(name = POKEMON_ID))
	@OrderColumn(name = POSITION)
	@Column(name = "ability")
	@BatchSize(size = PAGE_BATCH_SIZE)
	private List<String> abilities = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "local_pokemon_tag", joinColumns = @JoinColumn(name = POKEMON_ID))
	@Column(name = "tag")
	@BatchSize(size = PAGE_BATCH_SIZE)
	private Set<String> tags = new HashSet<>();

	@Transient
	private boolean isNew;

	protected LocalPokemonEntity() {
	}

	public LocalPokemonEntity(int id) {
		this.id = id;
		this.isNew = true;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markPersisted() {
		isNew = false;
	}

	public void copyFrom(LocalPokemon pokemon) {
		var upstream = pokemon.upstream();
		name = upstream.name();
		spriteUrl = upstream.spriteUrl();
		imageUrl = upstream.imageUrl();
		category = upstream.category();
		weightHectograms = upstream.weightHectograms();
		heightDecimetres = upstream.heightDecimetres();
		replace(types, upstream.types());
		replace(abilities, upstream.abilities());

		var proprietary = pokemon.proprietary();
		localizedName = proprietary.localizedName();
		region = proprietary.region();
		habitat = proprietary.habitat();
		notes = proprietary.notes();
		tags.retainAll(proprietary.tags());
		tags.addAll(proprietary.tags());

		syncedAt = pokemon.syncedAt();
		updatedAt = pokemon.updatedAt();
	}

	public LocalPokemon toDomain() {
		return new LocalPokemon(
			id,
			new UpstreamData(name, spriteUrl, imageUrl, category, weightHectograms, heightDecimetres, types, abilities),
			new ProprietaryData(localizedName, region, habitat, tags, notes),
			version,
			syncedAt,
			updatedAt);
	}

	private static void replace(List<String> target, List<String> values) {
		target.clear();
		target.addAll(values);
	}
}
