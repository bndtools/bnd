package aQute.bnd.component;

import static aQute.bnd.component.DSAnnotationReader.V1_0;
import static aQute.bnd.component.DSAnnotationReader.V1_3;
import static aQute.bnd.component.DSAnnotationReader.VMAX;
import static aQute.lib.strings.Strings.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;

import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError.ErrorType;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.collections.MultiMap;
import aQute.lib.strings.Strings;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class DSAnnotations implements AnalyzerPlugin {

	public enum Options {
		inherit,
		felixExtensions,
		extender,
		nocapabilities,
		norequirements,

		version {
			@Override
			void process(VersionSettings settings, Attrs attrs) {
				String min = attrs.get("minimum");
				if (min != null && min.length() > 0) {
					settings.minVersion = Version.valueOf(min);
				}
				String max = attrs.get("maximum");

				if (max != null && max.length() > 0) {
					settings.maxVersion = Version.valueOf(max);
				}
			}

		};

		void process(VersionSettings anno, Attrs attrs) {

		}

		void reset(VersionSettings anno) {

		}

		static void parseOption(Map.Entry<String, Attrs> entry, Set<Options> options, VersionSettings state) {
			String s = entry.getKey();
			boolean negation = false;
			if (s.startsWith("!")) {
				negation = true;
				s = s.substring(1);
			}
			Options option = Options.valueOf(s);
			if (negation) {
				options.remove(option);
				option.reset(state);
			} else {
				options.add(option);
				Attrs attrs;
				if ((attrs = entry.getValue()) != null) {
					option.process(state, attrs);
				}
			}

		}
	}

	static class VersionSettings {
		Version	minVersion	= V1_3;
		Version	maxVersion	= VMAX;
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		VersionSettings settings = new VersionSettings();

		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS, "*"));
		if (header.isEmpty()) {
			return false;
		}

		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.mergeProperties(Constants.DSANNOTATIONS_OPTIONS));
		Set<Options> options = EnumSet.noneOf(Options.class);
		for (Map.Entry<String, Attrs> entry : optionsHeader.entrySet()) {
			try {
				Options.parseOption(entry, options, settings);
			} catch (IllegalArgumentException e) {
				analyzer.error("Unrecognized %s value %s with attributes %s, expected values are %s",
					Constants.DSANNOTATIONS_OPTIONS, entry.getKey(), entry.getValue(), EnumSet.allOf(Options.class));
			}
		}
		// obsolete but backwards compatible, use the options instead
		if (analyzer.is("-dsannotations-inherit")) {
			options.add(Options.inherit);
		}
		if (analyzer.is("-ds-felix-extensions")) {
			options.add(Options.felixExtensions);
		}

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace()
			.values();
		String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
		List<String> componentPaths = new ArrayList<>();
		if (sc != null && sc.trim()
			.length() > 0) {
			componentPaths.add(sc);
		}
		boolean nouses = analyzer.is(Constants.NOUSES);

		MultiMap<String, ComponentDef> definitionsByName = new MultiMap<>();

		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();
		Version maxVersionUsedByAnyComponent = V1_0;

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);
		boolean componentProcessed = false;
		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}
					ComponentDef definition = DSAnnotationReader.getDefinition(c, analyzer, options, finder,
						settings.minVersion);
					if (definition == null) {
						break;
					}

					componentProcessed = true;
					definition.sortReferences();
					definition.prepare(analyzer);

					checkVersionConflicts(analyzer, definition, settings);

					//
					// we need a unique definition.name
					// according to the spec so we should deduplicate
					// these names
					//

					makeUnique(definitionsByName, definition);

					String path = "OSGI-INF/" + analyzer.validResourcePath(definition.name, "Invalid component name")
						+ ".xml";
					componentPaths.add(path);
					analyzer.getJar()
						.putResource(path, new TagResource(definition.getTag()));

					if (!options.contains(Options.nocapabilities)) {
						addServiceCapability(definition.service, provides, nouses);
					}

					if (!options.contains(Options.norequirements)) {
						MergedRequirement serviceReqMerge = new MergedRequirement("osgi.service");
						for (ReferenceDef ref : definition.references.values()) {
							addServiceRequirement(ref, serviceReqMerge);
						}
						requires.addAll(serviceReqMerge.toStringList());
					}
					maxVersionUsedByAnyComponent = ComponentDef.max(maxVersionUsedByAnyComponent, definition.version);

					break;
				}
			}
		}
		if (componentProcessed
			&& (options.contains(Options.extender) || (maxVersionUsedByAnyComponent.compareTo(V1_3) >= 0))) {
			Clazz componentAnnotation = analyzer
				.findClass(analyzer.getTypeRef("org/osgi/service/component/annotations/Component"));
			if ((componentAnnotation == null) || !componentAnnotation.annotations()
				.contains(
					analyzer.getTypeRef("org/osgi/service/component/annotations/RequireServiceComponentRuntime"))) {
				maxVersionUsedByAnyComponent = ComponentDef.max(maxVersionUsedByAnyComponent, V1_3);
				addExtenderRequirement(requires, maxVersionUsedByAnyComponent);
			}
		}
		componentPaths = removeOverlapInServiceComponentHeader(componentPaths);
		sc = Processor.append(componentPaths.toArray(new String[0]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		updateHeader(analyzer, Constants.REQUIRE_CAPABILITY, requires);
		updateHeader(analyzer, Constants.PROVIDE_CAPABILITY, provides);

		MapStream.of(definitionsByName)
			.filterValue(l -> l.size() > 1)
			.forEach((k, v) -> analyzer.error("Same component name %s used in multiple component implementations: %s",
				k, v.stream()
					.map(def -> def.implementation)
					.collect(toList())));
		return false;
	}

	/*
	 * Check for any version conflicts and report them as errors
	 */
	private void checkVersionConflicts(Analyzer analyzer, ComponentDef definition, VersionSettings settings) {

		if (definition.version.compareTo(settings.maxVersion) > 0) {
			DeclarativeServicesAnnotationError dse = new DeclarativeServicesAnnotationError(
				definition.implementation.getFQN(), null, ErrorType.VERSION_MISMATCH);
			analyzer
				.error("component %s version %s exceeds -dsannotations-options version;maximum version %s because %s",
					definition.version, definition.name, settings.maxVersion, definition.versionReason)
				.details(dse);
		}

	}

	private void makeUnique(MultiMap<String, ComponentDef> definitionsByName, ComponentDef definition) {
		String uniqueName = definition.name;
		List<ComponentDef> l = definitionsByName.getOrDefault(definition.name, Collections.emptyList());
		if (!l.isEmpty()) {
			uniqueName += "-" + l.size();
		}
		definitionsByName.add(definition.name, definition);
		definition.name = uniqueName;
	}

	public static List<String> removeOverlapInServiceComponentHeader(Collection<String> names) {
		List<String> wildcards = new ArrayList<>(names);
		wildcards.removeIf(name -> !name.contains("*"));

		Instructions wildcardedPaths = new Instructions(wildcards);
		if (wildcardedPaths.isEmpty())
			return new ArrayList<>(names);

		List<String> actual = new ArrayList<>();
		for (String name : names) {
			if (!name.contains("*") && wildcardedPaths.matches(name))
				continue;
			actual.add(name);
		}
		return actual;
	}

	private void addServiceCapability(TypeRef[] services, Set<String> provides, boolean nouses) {
		if (services == null) {
			return;
		}
		String objectClass = Arrays.stream(services)
			.map(TypeRef::getFQN)
			.sorted()
			.collect(joining());
		if (objectClass.isEmpty()) {
			return;
		}
		Attrs a = new Attrs();
		a.put(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE + ":List<String>", objectClass);
		if (!nouses) {
			String uses = Arrays.stream(services)
				.map(TypeRef::getPackageRef)
				.filter(pkg -> !pkg.isJava() && !pkg.isMetaData())
				.map(PackageRef::getFQN)
				.sorted()
				.collect(joining());
			if (!uses.isEmpty()) {
				a.put(Constants.USES_DIRECTIVE, uses);
			}
		}

		Parameters p = new Parameters();
		p.put(ServiceNamespace.SERVICE_NAMESPACE, a);
		String s = p.toString();
		provides.add(s);
	}

	private void addServiceRequirement(ReferenceDef ref, MergedRequirement requires) {
		String objectClass = ref.service;
		ReferenceCardinality cardinality = ref.cardinality;
		boolean optional = cardinality == ReferenceCardinality.OPTIONAL || cardinality == ReferenceCardinality.MULTIPLE;
		boolean multiple = cardinality == ReferenceCardinality.MULTIPLE
			|| cardinality == ReferenceCardinality.AT_LEAST_ONE;

		String filter = "(" + ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE + "=" + objectClass + ")";
		requires.put(filter, Namespace.EFFECTIVE_ACTIVE, optional, multiple);
	}

	private void addExtenderRequirement(Set<String> requires, Version version) {
		Version next = version.bumpMajor();
		Parameters p = new Parameters();
		Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE,
			"\"(&(" + ExtenderNamespace.EXTENDER_NAMESPACE + "=" + ComponentConstants.COMPONENT_CAPABILITY_NAME + ")("
				+ ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE + ">=" + version + ")(!("
				+ ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE + ">=" + next + ")))\"");
		p.put(ExtenderNamespace.EXTENDER_NAMESPACE, a);
		String s = p.toString();
		requires.add(s);
	}

	/**
	 * Updates specified header, sorting and removing duplicates. Destroys
	 * contents of set parameter.
	 *
	 * @param analyzer
	 * @param name header name
	 * @param set values to add to header; contents are not preserved.
	 */
	private void updateHeader(Analyzer analyzer, String name, TreeSet<String> set) {
		if (!set.isEmpty()) {
			String value = analyzer.getProperty(name);
			if (value != null) {
				Parameters p = OSGiHeader.parseHeader(value);
				for (Map.Entry<String, Attrs> entry : p.entrySet()) {
					StringBuilder sb = new StringBuilder(entry.getKey());
					if (entry.getValue() != null) {
						sb.append(";");
						entry.getValue()
							.append(sb);
					}
					set.add(sb.toString());
				}
			}
			String header = Strings.join(set);
			analyzer.setProperty(name, header);
		}
	}

	@Override
	public String toString() {
		return "DSAnnotations";
	}
}
