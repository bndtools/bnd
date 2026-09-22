package aQute.bnd.osgi;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.lib.utf8properties.UTF8Properties;
import aQute.lib.utf8properties.UTF8Properties.Property;
import aQute.bnd.osgi.PropertyConflict.Occurrence;
import aQute.service.reporter.Reporter.SetLocation;

final class PropertyConflictTracker {
	private final Map<String, List<Occurrence>> declarations = new LinkedHashMap<>();
	private final Map<String, PropertyConflict> includes = new LinkedHashMap<>();
	private final Set<String> messages = new LinkedHashSet<>();
	private boolean dirty;
	private String policy;

	void parsed(String source, List<Property> properties) {
		declarations.put(source, properties.stream()
			.map(property -> new Occurrence(property.key(), property.source(), property.line(), property.start(), property.end()))
			.toList());
		dirty = true;
	}

	void include(String key, Properties target, Properties incoming, File root, File file) {
		Occurrence previous = declaration(key, target, root);
		Occurrence winner = declaration(key, incoming, file);
		Set<Occurrence> occurrences = new LinkedHashSet<>();
		PropertyConflict existing = includes.get(key);
		if (existing != null)
			occurrences.addAll(existing.occurrences());
		occurrences.add(previous);
		occurrences.add(winner);
		includes.put(key, new PropertyConflict(PropertyConflict.Kind.INCLUDE, key, List.copyOf(occurrences), winner));
		dirty = true;
	}

	private Occurrence declaration(String key, Properties properties, File fallback) {
		String source = properties instanceof UTF8Properties utf8 ? utf8.getProvenance(key).orElse(null) : null;
		if (source == null)
			source = fallback == null ? "" : fallback.getAbsolutePath();
		List<Occurrence> parsed = declarations.getOrDefault(source, List.of());
		for (int index = parsed.size() - 1; index >= 0; index--) {
			Occurrence property = parsed.get(index);
			if (property.key().equals(key))
				return property;
		}
		return new Occurrence(key, source, 0, -1, -1);
	}

	List<PropertyConflict> conflicts() {
		List<PropertyConflict> result = new ArrayList<>();
		for (List<Occurrence> parsed : declarations.values()) {
			Map<String, List<Occurrence>> keys = parsed.stream()
				.collect(Collectors.groupingBy(Occurrence::key, LinkedHashMap::new, Collectors.toList()));
			keys.forEach((key, occurrences) -> {
				if (occurrences.size() > 1)
					result.add(new PropertyConflict(PropertyConflict.Kind.DUPLICATE, key, occurrences,
						occurrences.get(occurrences.size() - 1)));
			});
		}
		result.addAll(includes.values());
		return List.copyOf(result);
	}

	void reset(Processor processor) {
		messages.forEach(processor.reporter::remove);
		messages.clear();
		declarations.clear();
		includes.clear();
		dirty = true;
	}

	void report(Processor processor) {
		String configured = processor.getProperty(Constants.PROPERTYCONFLICTS);
		String severity = configured == null
			? (processor.isPedantic() || Processor.isTrue(processor.getProperty(Constants.PEDANTIC)) ? "warning" : "off")
			: configured.trim();
		if (!dirty && severity.equals(policy))
			return;
		dirty = false;
		policy = severity;
		messages.forEach(processor.reporter::remove);
		messages.clear();
		if (severity.equals("off"))
			return;
		if (!severity.equals("warning") && !severity.equals("error")) {
			String message = "Invalid -propertyconflicts value: " + severity + "; expected off, warning, or error";
			processor.error("%s", message).header(Constants.PROPERTYCONFLICTS);
			messages.add(message);
			return;
		}
		for (PropertyConflict conflict : conflicts()) {
			Occurrence location = conflict.kind() == PropertyConflict.Kind.DUPLICATE ? conflict.occurrences().get(1)
				: conflict.occurrences().get(0);
			String sources = conflict.occurrences().stream()
				.map(property -> property.source() + ":" + (property.line() + 1))
				.collect(Collectors.joining(", "));
			String message = "[Property Conflict]: `" + conflict.key() + "` "
				+ (conflict.kind() == PropertyConflict.Kind.DUPLICATE ? "is defined more than once" : "is shadowed by -include")
				+ " (" + sources + ")."
				+ (conflict.mergeable() ? " Use unique .<suffix> keys to merge values, or remove the unwanted definition."
					: " Remove or edit the unwanted definition.");
			SetLocation diagnostic = severity.equals("error") ? processor.error("%s", message)
				: processor.warning("%s", message);
			diagnostic.file(location.source()).line(location.line()).header(conflict.key()).details(conflict);
			messages.add(message);
		}
	}
}
