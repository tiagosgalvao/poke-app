package com.poke.catalog.controller;

import com.poke.catalog.controller.PokemonResponses.PokemonDetailResponse;
import com.poke.catalog.controller.PokemonResponses.PokemonSummaryResponse;
import com.poke.catalog.service.CatalogService;
import com.poke.shared.pagination.PageRequest;
import com.poke.shared.pagination.PageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pokemon")
class PokemonController {

	private static final String FIRST_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";

	private final CatalogService catalogService;
	private final PokemonResponseMapper mapper;

	PokemonController(CatalogService catalogService, PokemonResponseMapper mapper) {
		this.catalogService = catalogService;
		this.mapper = mapper;
	}

	@GetMapping
	PageResponse<PokemonSummaryResponse> browse(
		@RequestParam(defaultValue = FIRST_PAGE) int page,
		@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
		return PageResponse.from(catalogService.browse(new PageRequest(page, size)), mapper::toSummaryResponse);
	}

	@GetMapping("/{idOrName}")
	PokemonDetailResponse detail(@PathVariable String idOrName) {
		return mapper.toDetailResponse(catalogService.getDetail(idOrName));
	}
}
