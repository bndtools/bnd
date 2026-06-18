package bndtools.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class BndPreferencesTest {

	@Test
	public void parse_null_or_blank_yields_empty_list() {
		assertEquals(List.of(), BndPreferences.parseUriList(null));
		assertEquals(List.of(), BndPreferences.parseUriList(""));
		assertEquals(List.of(), BndPreferences.parseUriList("   \t  "));
	}

	@Test
	public void parse_splits_on_any_whitespace_and_trims() {
		assertEquals(List.of("a", "b", "c"), BndPreferences.parseUriList("  a   b \t c "));
	}

	@Test
	public void parse_keeps_a_single_value() {
		assertEquals(List.of("https://example.org/index.bnd"),
			BndPreferences.parseUriList("https://example.org/index.bnd"));
	}

	@Test
	public void format_joins_with_single_space_and_drops_blanks() {
		assertEquals("a b c", BndPreferences.formatUriList(Arrays.asList("a", "", "b", "   ", "c")));
		assertEquals("", BndPreferences.formatUriList(List.of()));
	}

	@Test
	public void round_trip_preserves_entries() {
		List<String> uris = List.of("https://a/index.bnd", "file:/tmp/b/index.bnd");
		assertEquals(uris, BndPreferences.parseUriList(BndPreferences.formatUriList(uris)));
	}
}
