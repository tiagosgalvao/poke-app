package com.poke.catalog.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlavorTextTest {

	@Test
	void joinsLinesAndFormFeedsIntoSingleSpaces() {
		var raw = "When several of\nthese POKéMON\ngather, their\felectricity could\nbuild and cause\nlightning storms.";

		assertThat(FlavorText.normalize(raw))
				.isEqualTo("When several of these POKéMON gather, their electricity could build and cause lightning storms.");
	}

	@Test
	void removesSoftHyphensIncludingTheLineBreakAfterThem() {
		assertThat(FlavorText.normalize("electri\u00ad\ncity and ener\u00adgy")).isEqualTo("electricity and energy");
	}

	@Test
	void collapsesWhitespaceAndTrims() {
		assertThat(FlavorText.normalize("  Possesses   cheek sacs. \n")).isEqualTo("Possesses cheek sacs.");
	}

	@Test
	void blankOrMissingTextBecomesNull() {
		assertThat(FlavorText.normalize(null)).isNull();
		assertThat(FlavorText.normalize(" \n\f ")).isNull();
	}
}
