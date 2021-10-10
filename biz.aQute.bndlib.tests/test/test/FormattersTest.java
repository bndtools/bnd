package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;

@SuppressWarnings("restriction")
public class FormattersTest {

	@Test
	public void testCollectionFormatter() {
		Converter<String, Collection<?>> formatter = new CollectionFormatter<>(",\\\n\t", (String) null);
		String formatted = formatter.convert(Arrays.asList("a", "b", "c"));
		assertEquals("\\\n\ta,\\\n\tb,\\\n\tc", formatted);
	}

	/*
	 * Don't add leading separator for single entries
	 */
	@Test
	public void testCollectionFormatterSingleEntry() {
		Converter<String, Collection<?>> formatter = new CollectionFormatter<>(",\\\n\t", (String) null);
		String formatted = formatter.convert(Arrays.asList("a"));
		assertEquals("a", formatted);
	}

}
