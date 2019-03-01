package aQute.bnd.component;

import static aQute.bnd.component.DSAnnotationReader.V1_0;
import static aQute.bnd.component.DSAnnotationReader.V1_3;
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
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.AnalyzerPlugin;
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
			void process(DSAnnotations anno, Attrs attrs) {
				String v = attrs.get("minimum");
				if (v != null && v.length() > 0) {
					anno.minVersion = new Version(v);
				}
			}

			@Override
			void reset(DSAnnotations anno) {
				anno.minVersion = V1_3;
			}
		};

		void process(DSAnnotations anno, Attrs attrs) {

		}

		void reset(DSAnnotations anno) {

		}

		static void parseOption(Map.Entry<String, Attrs> entry, EnumSet<Options> options, DSAnnotations state) {
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
	};

	Version minVersion;

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS, "*"));
		if (header.size() == 0) {
			return false;
		}

		minVersion = V1_3;
		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.mergeProperties(Constants.DSANNOTATIONS_OPTIONS));
		EnumSet<Options> options = EnumSet.noneOf(Options.class);
		for (Map.Entry<String, Attrs> entry : optionsHeader.entrySet()) {
			try {
				Options.parseOption(entry, options, this);
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

		MultiMap<String, ComponentDef> definitionsByName = new MultiMap<>();

		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();
		Version maxVersion = V1_0;

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);
		boolean componentProcessed = false;
		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}
					ComponentDef definition = DSAnnotationReader.getDefinition(c, analyzer, options, finder,
						minVersion);
					if (definition == null) {
						break;
					}
					componentProcessed = true;
					definition.sortReferences();
					definition.prepare(analyzer);

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

					if (definition.service != null && !options.contains(Options.nocapabilities)) {
						String[] objectClass = new String[definition.service.length];

						for (int i = 0; i < definition.service.length; i++) {
							Descriptors.TypeRef tr = definition.service[i];
							objectClass[i] = tr.getFQN();
						}
						Arrays.sort(objectClass);
						addServiceCapability(objectClass, provides);
					}

					if (!options.contains(Options.norequirements)) {
						MergedRequirement serviceReqMerge = new MergedRequirement("osgi.service");
						for (ReferenceDef ref : definition.references.values()) {
							addServiceRequirement(ref, serviceReqMerge);
						}
						requires.addAll(serviceReqMerge.toStringList());
					}
					maxVersion = ComponentDef.max(maxVersion, definition.version);
					break;
				}
			}
		}
		if (componentProcessed && (options.contains(Options.extender) || (maxVersion.compareTo(V1_3) >= 0))) {
			Clazz componentAnnotation = analyzer
				.findClass(analyzer.getTypeRef("org/osgi/service/component/annotations/Component"));
			if ((componentAnnotation == null) || !componentAnnotation.annotations()
				.contains(
					analyzer.getTypeRef("org/osgi/service/component/annotations/RequireServiceComponentRuntime"))) {
				maxVersion = ComponentDef.max(maxVersion, V1_3);
				addExtenderRequirement(requires, maxVersion);
			}
		}
		componentPaths = removeOverlapInServiceComponentHeader(componentPaths);
		sc = Processor.append(componentPaths.toArray(new String[0]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		updateHeader(analyzer, Constants.REQUIRE_CAPABILITY, requires);
		updateHeader(analyzer, Constants.PROVIDE_CAPABILITY, provides);

		definitionsByName.entrySet()
			.stream()
			.filter(e -> e.getValue()
				.size() > 1)
			.forEach(e -> {
				analyzer.error("Same component name %s used in multiple component implementations: %s",
					e.getKey(),
					e.getValue()
					.stream()
					.map(def -> def.implementation)
						.collect(toList()));
			});

		return false;
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

	private void addServiceCapability(String[] objectClass, Set<String> provides) {
		if (objectClass.length > 0) {
			Parameters p = new Parameters();
			Attrs a = new Attrs();
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String oc : objectClass) {
				sb.append(sep)
					.append(oc);
				sep = ",";
			}
			a.put("objectClass:List<String>", sb.toString());
			p.put("osgi.service", a);
			String s = p.toString();
			provides.add(s);
		}
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
