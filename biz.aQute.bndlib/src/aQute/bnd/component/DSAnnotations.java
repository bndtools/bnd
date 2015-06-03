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

	enum Options {
		inherit, felixExtensions, extender
	};

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
		if (header.size() == 0)
			return false;

		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS_OPTIONS));
		EnumSet<Options> options = EnumSet.noneOf(Options.class);
		for (String s : optionsHeader.keySet()) {
			try {
				options.add(Options.valueOf(s));
			}
			catch (IllegalArgumentException e) {
				analyzer.error("Unrecognized %s value %s, expected values are %s", Constants.DSANNOTATIONS_OPTIONS, s,
						EnumSet.allOf(Options.class));
			}
		}
		// obsolete but backwards compatible, use the options instead
		if (Processor.isTrue(analyzer.getProperty("-dsannotations-inherit")))
			options.add(Options.inherit);
		if (Processor.isTrue(analyzer.getProperty("-ds-felix-extensions")))
			options.add(Options.felixExtensions);


		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace().values();
		String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
		List<String> names = new ArrayList<String>();
		if (sc != null && sc.trim().length() > 0)
			names.add(sc);

		TreeSet<String> provides = new TreeSet<String>();
		TreeSet<String> requires = new TreeSet<String>();
		Version maxVersion = AnnotationReader.V1_0;

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);
		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					ComponentDef definition = AnnotationReader.getDefinition(c, analyzer, options, finder);
					if (definition != null) {

						definition.sortReferences();
						definition.prepare(analyzer);

						String name = "OSGI-INF/"
								+ analyzer.validResourcePath(definition.name, "Invalid component name") + ".xml";
						names.add(name);
						analyzer.getJar().putResource(name, new TagResource(definition.getTag()));

						if (definition.service != null) {
							String[] objectClass = new String[definition.service.length];

							for (int i = 0; i < definition.service.length; i++) {
								Descriptors.TypeRef tr = definition.service[i];
								objectClass[i] = tr.getFQN();
							}
							Arrays.sort(objectClass);
							addServiceCapability(objectClass, provides);
						}
						for (ReferenceDef ref : definition.references.values()) {
							addServiceRequirement(ref, requires);
						}
						maxVersion = ComponentDef.max(maxVersion, definition.version);
					}
				}
			}
		}
		if (options.contains(Options.extender)
				|| maxVersion.compareTo(AnnotationReader.V1_3) >= 0) {
			maxVersion = ComponentDef.max(maxVersion, AnnotationReader.V1_3);
			addExtenderRequirement(requires, maxVersion);
		}
		sc = Processor.append(names.toArray(new String[names.size()]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		updateHeader(analyzer, Constants.REQUIRE_CAPABILITY, requires);
		updateHeader(analyzer, Constants.PROVIDE_CAPABILITY, provides);
		return false;
	}

	private void addServiceCapability(String[] objectClass, Set<String> provides) {
		if (objectClass.length > 0) {
			Parameters p = new Parameters();
			Attrs a = new Attrs();
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String oc : objectClass) {
				sb.append(sep).append(oc);
				sep = ",";
			}
			a.put("objectClass:List<String>", sb.toString());
			p.put("osgi.service", a);
			String s = p.toString();
			provides.add(s);
		}
	}

	private void addServiceRequirement(ReferenceDef ref, Set<String> requires) {
		String objectClass = ref.service;
		ReferenceCardinality cardinality = ref.cardinality;
		boolean optional = cardinality == ReferenceCardinality.OPTIONAL || cardinality == ReferenceCardinality.MULTIPLE;
		boolean multiple = cardinality == ReferenceCardinality.MULTIPLE
				|| cardinality == ReferenceCardinality.AT_LEAST_ONE;
		Parameters p = new Parameters();
		Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE, "\"(objectClass=" + objectClass + ")\"");
		a.put(Constants.EFFECTIVE_DIRECTIVE, "\"active\"");
		if (optional) {
			a.put(Constants.RESOLUTION_DIRECTIVE, "\"optional\"");
		}
		if (multiple) {
			a.put("cardinality:", "\"multiple\"");
		}
		p.put("osgi.service", a);
		String s = p.toString();
		requires.add(s);
	}

	private void addExtenderRequirement(Set<String> requires, Version version) {
		Version next = new Version(version.getMajor() + 1);
		Parameters p = new Parameters();
		Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE, "\"(&(osgi.extender=osgi.component)(version>=" + version + ")(!(version>="
				+ next + ")))\"");
		p.put("osgi.extender", a);
		String s = p.toString();
		requires.add(s);
	}

	/**
	 * Updates specified header, sorting and removing duplicates. Destroys
	 * contents of set parameter.
	 * 
	 * @param analyzer
	 * @param name
	 *            header name
	 * @param set
	 *            values to add to header; contents are not preserved.
	 */
	private void updateHeader(Analyzer analyzer, String name, TreeSet<String> set) {
		if (!set.isEmpty()) {
			String value = analyzer.getProperty(name);
			if (value != null) {
				Parameters p = OSGiHeader.parseHeader(value);
				for (Map.Entry<String,Attrs> entry : p.entrySet()) {
					StringBuilder sb = new StringBuilder(entry.getKey());
					if (entry.getValue() != null) {
						sb.append(";");
						entry.getValue().append(sb);
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
