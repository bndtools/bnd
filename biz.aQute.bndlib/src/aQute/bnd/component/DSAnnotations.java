package aQute.bnd.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.component.annotations.ReferenceCardinality;

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
				String min = attrs.get("minimum");
				if (min != null && min.length() > 0) {
					anno.minVersion = new Version(min);
				}
				String max = attrs.get("maximum");
				if (max != null && max.length() > 0) {
					anno.maxVersion = new Version(max);
					if (anno.maxVersion.compareTo(anno.minVersion) < 0) {
						throw new IllegalArgumentException(
							"Error on " + version.name() + "attribute of "
							+ Constants.DSANNOTATIONS_OPTIONS
							+ ": maximum version must not be less than minimum version.");
					}
				}
			}

			@Override
			void reset(DSAnnotations anno) {
				anno.minVersion = AnnotationReader.V1_3;
				anno.maxVersion = null;
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
			Options option;
			try {
				option = Options.valueOf(s);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(String.format(
					"Unrecognized %s value %s with attributes %s, expected values are %s",
					Constants.DSANNOTATIONS_OPTIONS, entry.getKey(), entry.getValue(), EnumSet.allOf(Options.class)));
			}
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
	Version	maxVersion;

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS, "*"));
		if (header.size() == 0)
			return false;

		minVersion = AnnotationReader.V1_3;
		maxVersion = null;
		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.mergeProperties(Constants.DSANNOTATIONS_OPTIONS));
		EnumSet<Options> options = EnumSet.noneOf(Options.class);
		for (Map.Entry<String, Attrs> entry : optionsHeader.entrySet()) {
			try {
				Options.parseOption(entry, options, this);
			} catch (IllegalArgumentException e) {
				analyzer.error(e.getMessage());
			}
		}
		// obsolete but backwards compatible, use the options instead
		if (Processor.isTrue(analyzer.getProperty("-dsannotations-inherit")))
			options.add(Options.inherit);
		if (Processor.isTrue(analyzer.getProperty("-ds-felix-extensions")))
			options.add(Options.felixExtensions);

		// extender option conflicts with a maximum version less than 1.3
		if (options.contains(Options.extender) && maxVersion != null
			&& maxVersion.compareTo(AnnotationReader.V1_3) < 0) {
			analyzer.error(
				"Cannot use %s option in %s in conjunction with maximum version %s: the extender requirement supports only version %s and above.",
				Options.extender, Constants.DSANNOTATIONS_OPTIONS, maxVersion, AnnotationReader.V1_3);
		}

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace()
			.values();
		String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
		List<String> names = new ArrayList<>();
		if (sc != null && sc.trim()
			.length() > 0)
			names.add(sc);

		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();
		Version maxVersionFound = AnnotationReader.V1_0;

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);
		boolean componentProcessed = false;
		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}
					ComponentDef definition = AnnotationReader.getDefinition(c, analyzer, options, finder, minVersion);
					if (definition == null) {
						break;
					}
					if (maxVersion != null && maxVersion.compareTo(definition.version) < 0) {
						analyzer.error(
							"Component %s requires DS version %s, which exceeds the maximum allowed version %s.",
							definition.name, definition.version, maxVersion);
					}
					componentProcessed = true;
					definition.sortReferences();
					definition.prepare(analyzer);

					String name = "OSGI-INF/" + analyzer.validResourcePath(definition.name, "Invalid component name")
						+ ".xml";
					names.add(name);
					analyzer.getJar()
						.putResource(name, new TagResource(definition.getTag()));

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
					maxVersionFound = ComponentDef.max(maxVersionFound, definition.version);
					break;
				}
			}
		}
		if (componentProcessed
			&& (options.contains(Options.extender) || (maxVersionFound.compareTo(AnnotationReader.V1_3) >= 0))) {
			maxVersionFound = ComponentDef.max(maxVersionFound, AnnotationReader.V1_3);
			addExtenderRequirement(requires, maxVersionFound);
		}
		names = removeOverlapInServiceComponentHeader(names);
		sc = Processor.append(names.toArray(new String[0]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		updateHeader(analyzer, Constants.REQUIRE_CAPABILITY, requires);
		updateHeader(analyzer, Constants.PROVIDE_CAPABILITY, provides);
		return false;
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

		String filter = "(objectClass=" + objectClass + ")";
		requires.put(filter, "active", optional, multiple);
	}

	private void addExtenderRequirement(Set<String> requires, Version version) {
		Version next = version.bumpMajor();
		Parameters p = new Parameters();
		Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE,
			"\"(&(osgi.extender=osgi.component)(version>=" + version + ")(!(version>=" + next + ")))\"");
		p.put("osgi.extender", a);
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
