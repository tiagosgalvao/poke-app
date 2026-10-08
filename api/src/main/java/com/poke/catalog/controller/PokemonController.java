package com.poke.catalog.controller;

import com.poke.catalog.controller.PokemonResponses.PokemonDetailResponse;
import com.poke.catalog.controller.PokemonResponses.PokemonSummaryResponse;
import com.poke.catalog.service.CatalogService;
import com.poke.shared.pagination.PageRequest;
import com.poke.shared.pagination.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pokemon")
class PokemonController {

	private static final String FIRST_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";

	private final CatalogService catalogService;

	PokemonController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping
	PageResponse<PokemonSummaryResponse> browse(
			@RequestParam(defaultValue = FIRST_PAGE) int page,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
		return PageResponse.from(catalogService.browse(new PageRequest(page, size)), PokemonSummaryResponse::from);
	}

	@GetMapping("/{idOrName}")
	PokemonDetailResponse detail(@PathVariable String idOrName) {
		return PokemonDetailResponse.from(catalogService.getDetail(idOrName));
	}
}
