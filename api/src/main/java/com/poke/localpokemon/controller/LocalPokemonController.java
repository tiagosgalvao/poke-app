package com.poke.localpokemon.controller;

import com.poke.localpokemon.controller.LocalPokemonRequests.ImportRequest;
import com.poke.localpokemon.controller.LocalPokemonRequests.ProprietaryPatchRequest;
import com.poke.localpokemon.controller.LocalPokemonRequests.ProprietaryUpdateRequest;
import com.poke.localpokemon.controller.LocalPokemonRequests.SyncRequest;
import com.poke.localpokemon.controller.LocalPokemonResponses.LocalPokemonResponse;
import com.poke.localpokemon.controller.LocalPokemonResponses.SyncSummaryResponse;
import com.poke.localpokemon.service.LocalPokemonService;
import com.poke.shared.pagination.PageRequest;
import com.poke.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/local-pokemon")
class LocalPokemonController {

	private static final String FIRST_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "20";
	private static final String BY_ID = "/{id}";

	private final LocalPokemonService localPokemonService;

	LocalPokemonController(LocalPokemonService localPokemonService) {
		this.localPokemonService = localPokemonService;
	}

	@GetMapping
	PageResponse<LocalPokemonResponse> list(
		@RequestParam(defaultValue = FIRST_PAGE) int page,
		@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
		return PageResponse.from(localPokemonService.list(new PageRequest(page, size)), LocalPokemonResponse::from);
	}

	@GetMapping(BY_ID)
	LocalPokemonResponse get(@PathVariable int id) {
		return LocalPokemonResponse.from(localPokemonService.get(id));
	}

	@PostMapping
	@ResponseStatus(CREATED)
	LocalPokemonResponse importPokemon(@Valid @RequestBody ImportRequest request) {
		return LocalPokemonResponse.from(localPokemonService.importPokemon(request.idOrName()));
	}

	@PutMapping(BY_ID)
	LocalPokemonResponse update(@PathVariable int id, @Valid @RequestBody ProprietaryUpdateRequest request) {
		return LocalPokemonResponse.from(localPokemonService.update(id, request.version(), request.toProprietaryData()));
	}

	@PatchMapping(BY_ID)
	LocalPokemonResponse patch(@PathVariable int id, @Valid @RequestBody ProprietaryPatchRequest request) {
		return LocalPokemonResponse.from(localPokemonService.patch(id, request.version(), request.toPatch()));
	}

	@DeleteMapping(BY_ID)
	@ResponseStatus(NO_CONTENT)
	void delete(@PathVariable int id) {
		localPokemonService.delete(id);
	}

	@PostMapping("/sync")
	SyncSummaryResponse sync(@Valid @RequestBody SyncRequest request) {
		return SyncSummaryResponse.from(localPokemonService.sync(request.toBatch()));
	}
}
