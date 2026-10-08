package com.poke.catalog.client;

import java.util.regex.Pattern;

final class FlavorText {

	private static final String SOFT_HYPHEN = "\u00ad";
	private static final String SOFT_HYPHEN_AT_LINE_END = SOFT_HYPHEN + "\n";
	private static final String NOTHING = "";
	private static final String SPACE = " ";
	private static final Pattern LINE_BREAKS_AND_FORM_FEEDS = Pattern.compile("[\\n\\r\\f]");
	private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s{2,}");

	private FlavorText() {
	}

	static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		var withoutSoftHyphens = raw.replace(SOFT_HYPHEN_AT_LINE_END, NOTHING).replace(SOFT_HYPHEN, NOTHING);
		var singleLine = LINE_BREAKS_AND_FORM_FEEDS.matcher(withoutSoftHyphens).replaceAll(SPACE);
		var normalized = REPEATED_WHITESPACE.matcher(singleLine).replaceAll(SPACE).strip();
		return normalized.isEmpty() ? null : normalized;
	}
}
