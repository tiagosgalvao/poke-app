package com.poke.catalog.client;

import com.poke.catalog.client.dto.EvolutionChainDto.ChainLink;
import com.poke.catalog.client.dto.EvolutionChainDto.EvolutionDetail;
import com.poke.catalog.domain.EvolutionStage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static com.poke.catalog.client.dto.NullSafeLists.orEmpty;

final class EvolutionChainFlattener {

	static final String SPRITE_URL_TEMPLATE =
		"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";

	private static final String TRIGGER_DETAIL_SEPARATOR = ": ";

	private EvolutionChainFlattener() {
	}

	static List<EvolutionStage> flatten(ChainLink root) {
		if (root == null) {
			return List.of();
		}
		var stages = new ArrayList<EvolutionStage>();
		var pending = new ArrayDeque<PendingLink>();
		pending.add(new PendingLink(root, 0, null));
		while (!pending.isEmpty()) {
			var current = pending.poll();
			int id = current.link().species().id();
			stages.add(new EvolutionStage(
				current.stage(),
				id,
				current.link().species().name(),
				current.evolvesFromId(),
				current.stage() == 0 ? null : describeTrigger(current.link().evolutionDetails()),
				SPRITE_URL_TEMPLATE.formatted(id)));
			for (var next : orEmpty(current.link().evolvesTo())) {
				pending.add(new PendingLink(next, current.stage() + 1, id));
			}
		}
		return List.copyOf(stages);
	}

	static String describeTrigger(List<EvolutionDetail> details) {
		var detail = orEmpty(details).stream().findFirst().orElse(null);
		if (detail == null || detail.trigger() == null) {
			return null;
		}
		var trigger = detail.trigger().name();
		if (detail.item() != null) {
			return trigger + TRIGGER_DETAIL_SEPARATOR + detail.item().name();
		}
		if (detail.minLevel() != null) {
			return trigger + TRIGGER_DETAIL_SEPARATOR + detail.minLevel();
		}
		return trigger;
	}

	private record PendingLink(ChainLink link, int stage, Integer evolvesFromId) {
	}
}
