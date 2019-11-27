package aQute.bnd.component;

import static java.util.stream.Collectors.toList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.libg.tuple.Pair;

/**
 * Merge together requirements having the same filter: and effective:
 * directives, but possibly differing in optionality/cardinality. Any mandatory
 * requirement will override an optional requirement, and any multiple
 * cardinality will override single.
 */
public class MergedRequirement {

	private static final String	MULTIPLE	= "\"multiple\"";
	private static final String	OPTIONAL	= "\"optional\"";

	private static class FilterEffectivePair extends Pair<String, String> {
		private static final long serialVersionUID = 1L;

		FilterEffectivePair(String filter, String effective) {
			super(filter, effective);
		}
	}

	private final Map<FilterEffectivePair, Attrs>	filterMap	= new LinkedHashMap<>();
	private final String							namespace;

	public MergedRequirement(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Add a requirement to the mix.
	 */
	public void put(String filter, String effective, boolean optional, boolean multiple) {
		FilterEffectivePair key = new FilterEffectivePair(filter, effective);
		Attrs existing = filterMap.get(key);
		if (existing != null) {
			// any mandatory requirement makes the whole thing mandatory
			boolean existingOptional = OPTIONAL.equals(existing.get(Constants.RESOLUTION_DIRECTIVE));
			optional = optional && existingOptional;

			// any single multiple requirement makes the whole thing multiple
			boolean existingMultiple = MULTIPLE.equals(existing.get(Constants.CARDINALITY_DIRECTIVE));
			multiple = multiple || existingMultiple;
		}

		Attrs newAttrs = new Attrs();
		newAttrs.put(Constants.FILTER_DIRECTIVE, '"' + filter + '"');
		if (effective != null)
			newAttrs.put(Constants.EFFECTIVE_DIRECTIVE, '"' + effective + '"');

		if (optional)
			newAttrs.put(Constants.RESOLUTION_DIRECTIVE, OPTIONAL);

		if (multiple)
			newAttrs.put(Constants.CARDINALITY_DIRECTIVE, MULTIPLE);
		filterMap.put(key, newAttrs);
	}

	/**
	 * Generate a list of strings formatted appropriately as entries in the
	 * Require-Capability header.
	 */
	public List<String> toStringList() {
		List<String> strings = filterMap.values()
			.stream()
			.map(attrs -> new StringBuilder().append(namespace)
				.append(';')
				.append(attrs)
				.toString())
			.collect(toList());
		return strings;
	}

}
