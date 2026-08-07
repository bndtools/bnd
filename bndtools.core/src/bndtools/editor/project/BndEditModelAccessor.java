package bndtools.editor.project;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.resource.Requirement;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.build.model.conversions.HeaderClauseListConverter;
import aQute.bnd.build.model.conversions.MapFormatter;
import aQute.bnd.build.model.conversions.PropertiesConverter;
import aQute.bnd.build.model.conversions.PropertiesEntryFormatter;
import aQute.bnd.build.model.conversions.RequirementFormatter;
import aQute.bnd.build.model.conversions.RequirementListConverter;
import aQute.bnd.build.model.conversions.VersionedClauseConverter;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.PropertyKey;

/** Convenience accessors for {@link BndEditModel} used by UI components that need to distinguish local vs inherited properties. */
class BndEditModelAccessor {

	private static final RequirementListConverter requirementListConverter = new RequirementListConverter();

	private static final Converter<String, Collection<? extends Requirement>> requirementListFormatter =
		new CollectionFormatter<>(BndEditModel.LIST_SEPARATOR, new RequirementFormatter(), null);

	private static final Converter<List<VersionedClause>, String> versionedClauseListConverter =
		new HeaderClauseListConverter<>(new VersionedClauseConverter());

	private static final Converter<String, Collection<? extends HeaderClause>> versionedClauseListFormatter =
		new CollectionFormatter<HeaderClause>(BndEditModel.LIST_SEPARATOR, new HeaderClauseFormatter(), null);

	private static final Converter<Map<String, String>, String> propertiesConverter = new PropertiesConverter();

	private static final Converter<String, Map<String, String>> propertiesFormatter =
		new MapFormatter(BndEditModel.LIST_SEPARATOR, new PropertiesEntryFormatter(), null);

	/** Returns true if the given property key is defined locally (not inherited from an included file). */
	static boolean isLocalProperty(BndEditModel model, String key) {
		return model.getDocumentProperties().containsKey(key);
	}

	/** Returns true if any local property key matches the stem or a stem.* variant. */
	static boolean hasLocalMergeProperty(BndEditModel model, String stem) {
		return !getLocalMergeKeys(model, stem).isEmpty();
	}

	/** Returns requirements merged across all stem.* variants from the owner processor (includes inherited files). */
	static List<Requirement> getMergedRequirements(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		String merged = p.mergeProperties(stem);
		if (merged == null || merged.isBlank())
			return null;
		return requirementListConverter.convert(merged);
	}

	/**
	 * Returns requirements from all local document merge keys (stem and stem.*).
	 * Reads from the saved document state; uncommitted in-memory drops are tracked
	 * separately in the viewer and committed on save.
	 */
	static List<Requirement> getLocalMergeRequirements(BndEditModel model, String stem) {
		Set<String> keys = getLocalMergeKeys(model, stem);
		if (keys.isEmpty())
			return Collections.emptyList();
		String merged = keys.stream()
			.map(model::getGenericString)
			.filter(v -> v != null && !v.isBlank())
			.collect(Collectors.joining(","));
		if (merged.isBlank())
			return Collections.emptyList();
		return requirementListConverter.convert(merged);
	}

	/** Returns requirements to write to an arbitrary merge key using the standard requirements format. */
	static void setRequirementListByKey(BndEditModel model, String key, List<Requirement> requires) {
		model.setGenericString(key, requirementListFormatter.convert(requires));
	}

	// ---- VersionedClause (runbundles, buildpath) ----------------------------

	/** Returns merged VersionedClause list from the owner processor (includes inherited files). */
	static List<VersionedClause> getMergedVersionedClauses(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		String merged = p.mergeProperties(stem);
		if (merged == null || merged.isBlank())
			return null;
		return versionedClauseListConverter.convert(merged);
	}

	/** Returns VersionedClause items from all local document merge keys (stem and stem.*). */
	static List<VersionedClause> getLocalVersionedClauses(BndEditModel model, String stem) {
		Set<String> keys = getLocalMergeKeys(model, stem);
		if (keys.isEmpty())
			return null;
		String merged = keys.stream()
			.map(model::getGenericString)
			.filter(v -> v != null && !v.isBlank())
			.collect(Collectors.joining(","));
		if (merged.isBlank())
			return null;
		return versionedClauseListConverter.convert(merged);
	}

	/** Writes a VersionedClause list to an arbitrary merge key. */
	static void setVersionedClausesByKey(BndEditModel model, String key, List<VersionedClause> clauses) {
		model.setGenericString(key, versionedClauseListFormatter.convert(clauses));
	}

	// ---- Map<String,String> (runproperties) ---------------------------------

	/** Returns merged properties map from the owner processor (includes inherited files). */
	static Map<String, String> getMergedProperties(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		String merged = p.mergeProperties(stem);
		if (merged == null || merged.isBlank())
			return null;
		return OSGiHeader.parseProperties(merged);
	}

	/** Returns properties map from all local document merge keys (stem and stem.*). */
	static Map<String, String> getLocalProperties(BndEditModel model, String stem) {
		Set<String> keys = getLocalMergeKeys(model, stem);
		if (keys.isEmpty())
			return null;
		String merged = keys.stream()
			.map(model::getGenericString)
			.filter(v -> v != null && !v.isBlank())
			.collect(Collectors.joining(","));
		if (merged.isBlank())
			return null;
		return OSGiHeader.parseProperties(merged);
	}

	/** Writes a properties map to an arbitrary merge key. */
	static void setPropertiesByKey(BndEditModel model, String key, Map<String, String> props) {
		model.setGenericString(key, propertiesFormatter.convert(props));
	}

	/**
	 * Returns per-entry provenance for inherited properties: maps each inherited property key
	 * to the absolute path of the file where that specific merge key was defined.
	 * First definition wins (matches bnd merge semantics).
	 */
	static Map<String, String> getInheritedPropertiesProvenance(BndEditModel model, String stem) {
		return getInheritedEntryProvenances(model, stem,
			raw -> OSGiHeader.parseProperties(raw).keySet());
	}

	/**
	 * Returns per-bundle provenance for inherited bundles: maps each BSN to the absolute path
	 * of the file where that bundle was defined.
	 */
	static Map<String, String> getInheritedBundleProvenances(BndEditModel model, String stem) {
		return getInheritedEntryProvenances(model, stem, raw -> {
			List<VersionedClause> clauses = versionedClauseListConverter.convert(raw);
			if (clauses == null)
				return Collections.emptySet();
			Set<String> bsns = new LinkedHashSet<>();
			for (VersionedClause vc : clauses)
				bsns.add(vc.getName());
			return bsns;
		});
	}

	/**
	 * Returns per-requirement provenance: maps each inherited Requirement to the absolute path
	 * of the file where it was defined.
	 */
	static Map<Requirement, String> getInheritedRequirementProvenances(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return Collections.emptyMap();
		Set<String> localDocKeys = getLocalMergeKeys(model, stem);
		Map<Requirement, String> result = new LinkedHashMap<>();
		for (PropertyKey pk : PropertyKey.findVisible(p.getMergePropertyKeys(stem))) {
			if (localDocKeys.contains(pk.key()))
				continue;
			Optional<String> prov = pk.getProvenance();
			if (prov.isEmpty())
				continue;
			String raw = pk.getRawValue();
			if (raw == null || raw.isBlank())
				continue;
			List<Requirement> reqs = requirementListConverter.convert(raw);
			if (reqs != null)
				for (Requirement req : reqs)
					result.putIfAbsent(req, prov.get());
		}
		return result;
	}

	/** Shared helper: maps each string entry (key/bsn) to its provenance file. */
	private static Map<String, String> getInheritedEntryProvenances(BndEditModel model, String stem,
		java.util.function.Function<String, java.util.Collection<String>> entryExtractor) {
		Processor p = model.getOwner();
		if (p == null)
			return Collections.emptyMap();
		Set<String> localDocKeys = getLocalMergeKeys(model, stem);
		Map<String, String> result = new LinkedHashMap<>();
		for (PropertyKey pk : PropertyKey.findVisible(p.getMergePropertyKeys(stem))) {
			if (localDocKeys.contains(pk.key()))
				continue;
			Optional<String> prov = pk.getProvenance();
			if (prov.isEmpty())
				continue;
			String raw = pk.getRawValue();
			if (raw == null || raw.isBlank())
				continue;
			for (String entry : entryExtractor.apply(raw))
				result.putIfAbsent(entry, prov.get());
		}
		return result;
	}

	// ---- String (runvm, runprogramargs) -------------------------------------

	/** Returns merged string value from the owner processor (includes inherited files). */
	static String getMergedString(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		return p.mergeProperties(stem);
	}

	// ---- Local key resolution -----------------------------------------------

	/**
	 * Returns the first existing local merge key for stem (plain stem takes precedence over stem.*),
	 * or {@code null} if no local key exists.
	 */
	static String findLocalMergeKey(BndEditModel model, String stem) {
		Set<String> keys = getLocalMergeKeys(model, stem);
		if (keys.contains(stem))
			return stem;
		return keys.stream().findFirst().orElse(null);
	}

	/** Returns the provenance (file path) of the first visible merge key for the given stem, or empty if local/unknown. */
	static Optional<String> getPropertyProvenance(BndEditModel model, String key) {
		Processor p = model.getOwner();
		if (p == null)
			return Optional.empty();
		return PropertyKey.findVisible(p.getMergePropertyKeys(key))
			.stream()
			.findFirst()
			.flatMap(PropertyKey::getProvenance);
	}

	/** Returns all local document property keys matching {@code stem} or {@code stem.*}. */
	static Set<String> getLocalMergeKeys(BndEditModel model, String stem) {
		String prefix = stem + ".";
		Properties docProps = model.getDocumentProperties();
		Set<String> keys = new LinkedHashSet<>();
		docProps.stringPropertyNames()
			.stream()
			.filter(k -> k.equals(stem) || k.startsWith(prefix))
			.forEach(keys::add);
		return keys;
	}

	private BndEditModelAccessor() {}
}
