package com.poke.catalog.service;

import com.poke.catalog.domain.PokemonCatalog;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

	@Mock
	PokemonCatalog catalog;

	@InjectMocks
	CatalogService service;

	@Test
	void browseDelegatesTheRequestedPage() {
		var request = new PageRequest(2, 10);
		var page = new Page<>(List.of(summary(21)), 2, 10, 1351);
		when(catalog.findPage(request)).thenReturn(page);

		assertThat(service.browse(request)).isSameAs(page);
	}

	@Test
	void browseRequiresARequest() {
		assertThatThrownBy(() -> service.browse(null)).isInstanceOf(DomainValidationException.class);
		verifyNoInteractions(catalog);
	}

	@Test
	void getDetailLooksUpTheNormalizedKey() {
		var pikachu = detail(25);
		when(catalog.findDetail("pikachu")).thenReturn(Optional.of(pikachu));

		assertThat(service.getDetail("  Pikachu ")).isSameAs(pikachu);
		verify(catalog).findDetail("pikachu");
	}

	@Test
	void getDetailStripsLeadingZerosFromIds() {
		when(catalog.findDetail("25")).thenReturn(Optional.of(detail(25)));

		assertThat(service.getDetail("025").id()).isEqualTo(25);
	}

	@Test
	void getDetailFailsWhenTheCatalogHasNoSuchPokemon() {
		when(catalog.findDetail("missingno")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDetail("missingno"))
				.isInstanceOf(PokemonNotFoundException.class)
				.hasMessageContaining("missingno");
	}

	@Test
	void getDetailRejectsMalformedKeysWithoutCallingTheCatalog() {
		assertThatThrownBy(() -> service.getDetail("mr. mime")).isInstanceOf(DomainValidationException.class);
		verifyNoInteractions(catalog);
	}

	@Test
	void upstreamFailuresPropagateUnchanged() {
		var failure = new ExternalServiceUnavailableException("PokeAPI is unavailable", null);
		when(catalog.findPage(any())).thenThrow(failure);

		assertThatThrownBy(() -> service.browse(new PageRequest(0, 20))).isSameAs(failure);
	}

	private static PokemonSummary summary(int id) {
		return new PokemonSummary(id, "pokemon-" + id, null, null, 10, 10, List.of(), List.of());
	}

	private static PokemonDetail detail(int id) {
		return new PokemonDetail(id, "pikachu", null, null, 60, 4, null, null, null, null, null);
	}
}
