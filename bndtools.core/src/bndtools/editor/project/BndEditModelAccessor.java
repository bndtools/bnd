package bndtools.editor.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.resource.Requirement;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseListConverter;
import aQute.bnd.build.model.conversions.RequirementListConverter;
import aQute.bnd.build.model.conversions.VersionedClauseConverter;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.PropertyKey;

/** Convenience accessors for {@link BndEditModel} used by UI components that need to distinguish local vs inherited properties. */
class BndEditModelAccessor {

	private static final RequirementListConverter requirementListConverter = new RequirementListConverter();

	private static final Converter<List<VersionedClause>, String> versionedClauseListConverter =
		new HeaderClauseListConverter<>(new VersionedClauseConverter());

	/** Returns true if any local property key matches the stem or a stem.* variant. */
	static boolean hasLocalMergeProperty(BndEditModel model, String stem) {
		return !getLocalMergeKeys(model, stem).isEmpty();
	}

	/**
	 * Returns requirements merged across all stem.* variants from the owner processor (includes inherited
	 * files), using each key's raw (unexpanded) text so a clause that is itself a single macro reference
	 * (e.g. {@code ${name}}) stays collapsed instead of being expanded into its constituent requirements.
	 */
	static List<Requirement> getMergedRequirements(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		List<PropertyKey> keys = PropertyKey.findVisible(p.getMergePropertyKeys(stem));
		if (keys.isEmpty())
			return null;
		List<Requirement> result = new ArrayList<>();
		for (PropertyKey pk : keys) {
			String raw = pk.getRawValue();
			if (raw == null || raw.isBlank())
				continue;
			List<Requirement> reqs = requirementListConverter.convert(raw);
			if (reqs != null)
				result.addAll(reqs);
		}
		return result.isEmpty() ? null : result;
	}

	/**
	 * Returns requirements from all local document merge keys (stem and stem.*).
	 * Reads from the saved document state; uncommitted in-memory drops are tracked
	 * separately in the viewer and committed on save.
	 */
	static List<Requirement> getLocalMergeRequirements(BndEditModel model, String stem) {
		List<Requirement> result = new ArrayList<>();
		for (String key : getLocalMergeKeys(model, stem)) {
			List<Requirement> reqs = model.getTypedProperty(key);
			if (reqs != null)
				result.addAll(reqs);
		}
		return result;
	}

	/** Writes requirements to an arbitrary merge key using the formatter registered for the stem. */
	static void setRequirementListByKey(BndEditModel model, String key, List<Requirement> requires) {
		model.setTypedProperty(key, requires);
	}

	/** Matches a requirement clause consisting of nothing but a single, unexpanded macro reference, e.g. {@code ${name}}. */
	private static final Pattern MACRO_REFERENCE = Pattern.compile("^\\$\\{([^${}]+)\\}$");

	/**
	 * Returns the referenced property/macro name if {@code req} is a single unexpanded macro
	 * reference clause (e.g. {@code ${fea_org.eclipse.e4.rcp_4.35.0.v20250228-0640}}), or empty
	 * otherwise.
	 */
	static Optional<String> getMacroReferenceName(Requirement req) {
		if (req == null)
			return Optional.empty();
		Matcher m = MACRO_REFERENCE.matcher(req.getNamespace());
		return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
	}

	/** Matches a single {@code ${name}} (or {@code ${name;arg;...}}) macro token, non-nested. */
	private static final Pattern MACRO_TOKEN = Pattern.compile("\\$\\{([^{}]*)\\}");

	/**
	 * Returns the raw (one-level) definition text of the given macro/property name, i.e. what is
	 * literally written for it (merged across its stem.* variants), without recursively expanding
	 * references to other defined properties. Any embedded built-in bnd macros that are not
	 * themselves defined properties (e.g. {@code ${workspace}}) are resolved to their actual value,
	 * since those are short and useful, while references to other (potentially large) properties are
	 * left as literal {@code ${name}} text so the result stays compact.
	 */
	static String getMacroDefinitionText(BndEditModel model, String macroName) {
		Processor p = model.getOwner();
		if (p == null)
			return null;
		List<PropertyKey> keys = PropertyKey.findVisible(p.getMergePropertyKeys(macroName));
		if (keys.isEmpty())
			return null;
		StringBuilder combined = new StringBuilder();
		for (PropertyKey pk : keys) {
			String raw = pk.getRawValue();
			if (raw == null || raw.isBlank())
				continue;
			if (combined.length() > 0)
				combined.append(',');
			combined.append(raw);
		}
		if (combined.length() == 0)
			return null;
		// One clause per line so the tooltip wraps readably (bnd's own line-continuation
		// backslash-newlines are collapsed away by the properties parser).
		String oneClausePerLine = String.join("\n", splitClauses(combined.toString()));
		return resolveBuiltinMacros(p, oneClausePerLine);
	}

	/** Splits a comma-separated header-clause-like value into its top-level clauses, respecting quotes and parens/braces. */
	static List<String> splitClauses(String raw) {
		List<String> result = new ArrayList<>();
		if (raw == null)
			return result;
		StringBuilder cur = new StringBuilder();
		int depth = 0;
		boolean inSingle = false, inDouble = false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '\'' && !inDouble) {
				inSingle = !inSingle;
			} else if (c == '"' && !inSingle) {
				inDouble = !inDouble;
			} else if (!inSingle && !inDouble) {
				if (c == '(' || c == '{')
					depth++;
				else if (c == ')' || c == '}')
					depth--;
				else if (c == ',' && depth == 0) {
					String clause = cur.toString()
						.strip();
					if (!clause.isEmpty())
						result.add(clause);
					cur.setLength(0);
					continue;
				}
			}
			cur.append(c);
		}
		String last = cur.toString()
			.strip();
		if (!last.isEmpty())
			result.add(last);
		return result;
	}

	/**
	 * Returns a display string for the given provenance file path: if this model's own {@code -include}
	 * entry (raw, unexpanded) resolves to that file, the literal include entry is returned instead
	 * (keeping any macro such as {@code ${workspace}} unresolved); otherwise the absolute path itself.
	 */
	static String getDisplayProvenance(BndEditModel model, String absolutePath) {
		if (absolutePath == null)
			return null;
		Processor p = model.getOwner();
		if (p == null)
			return absolutePath;
		// The owner processor removes -include from its properties once processed, so read the
		// raw (unexpanded) value from the edited document instead.
		String includeRaw = model.getDocumentProperties()
			.getProperty(aQute.bnd.osgi.Constants.INCLUDE);
		if (includeRaw == null || includeRaw.isBlank())
			return absolutePath;
		java.io.File target = new java.io.File(absolutePath);
		for (String clause : splitClauses(includeRaw)) {
			// Strip the optional leading modifier characters bnd allows on -include entries (~, -, !).
			String path = clause.replaceFirst("^[~\\-!]+", "");
			String resolved = p.getReplacer()
				.process(path);
			java.io.File resolvedFile = p.getFile(resolved);
			if (resolvedFile == null)
				continue;
			try {
				if (resolvedFile.getCanonicalFile()
					.equals(target.getCanonicalFile()))
					return clause;
			} catch (java.io.IOException ignore) {
				// fall through to next candidate
			}
		}
		return absolutePath;
	}

	/** Resolves only "built-in" macros (not backed by a defined property) found in {@code raw}. */
	private static String resolveBuiltinMacros(Processor p, String raw) {
		if (raw == null || raw.indexOf("${") < 0)
			return raw;
		Matcher m = MACRO_TOKEN.matcher(raw);
		StringBuilder out = new StringBuilder();
		int last = 0;
		while (m.find()) {
			String name = m.group(1)
				.split(";", 2)[0].trim();
			out.append(raw, last, m.start());
			if (name.isEmpty() || !p.getMergePropertyKeys(name).isEmpty()) {
				// Empty, or a reference to another defined (potentially large) property: keep collapsed.
				out.append(m.group());
			} else {
				// A built-in/system macro (e.g. ${workspace}): resolve to its actual value.
				out.append(p.getReplacer().process(m.group()));
			}
			last = m.end();
		}
		out.append(raw.substring(last));
		return out.toString();
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
		List<VersionedClause> result = new ArrayList<>();
		for (String key : keys) {
			List<VersionedClause> clauses = model.getTypedProperty(key);
			if (clauses != null)
				result.addAll(clauses);
		}
		return result;
	}

	/** Writes a VersionedClause list to an arbitrary merge key using the formatter registered for the stem. */
	static void setVersionedClausesByKey(BndEditModel model, String key, List<VersionedClause> clauses) {
		model.setTypedProperty(key, clauses);
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
		Map<String, String> result = new LinkedHashMap<>();
		for (String key : keys) {
			Map<String, String> props = model.getTypedProperty(key);
			if (props != null)
				result.putAll(props);
		}
		return result;
	}

	/** Writes a properties map to an arbitrary merge key using the formatter registered for the stem. */
	static void setPropertiesByKey(BndEditModel model, String key, Map<String, String> props) {
		model.setTypedProperty(key, props);
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
			// Use the raw (unexpanded) value so a clause that is itself a macro reference stays
			// collapsed and matches the entries produced by getMergedRequirements.
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
			// Use the macro-expanded value (not the raw text) so parsed entries match the merged view.
			String raw = pk.getValue();
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

	/** Returns visible non-local merge keys for the stem that carry a provenance (inherited from included files). */
	static List<PropertyKey> getInheritedPropertyKeys(BndEditModel model, String stem) {
		Processor p = model.getOwner();
		if (p == null)
			return Collections.emptyList();
		Set<String> localDocKeys = getLocalMergeKeys(model, stem);
		List<PropertyKey> result = new ArrayList<>();
		for (PropertyKey pk : PropertyKey.findVisible(p.getMergePropertyKeys(stem))) {
			if (localDocKeys.contains(pk.key()))
				continue;
			if (pk.getProvenance()
				.isPresent())
				result.add(pk);
		}
		return result;
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
