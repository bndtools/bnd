package aQute.bnd.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import aQute.bnd.osgi.Analyzer;
import aQute.lib.tag.Tag;

public class PropertiesDef {
	private final List<String> properties = new ArrayList<>();

	PropertiesDef(Analyzer analyzer) {}

	boolean isEmpty() {
		return properties.isEmpty();
	}

	PropertiesDef addProperties(String... props) {
		if (props != null) {
			Collections.addAll(properties, props);
		}
		return this;
	}

	Stream<Tag> propertiesTags(String element) {
		return properties.stream()
			.map(p -> new Tag(element).addAttribute("entry", p));
	}

	Stream<String> stream() {
		return properties.stream();
	}

	@Override
	public String toString() {
		return properties.toString();
	}
}
