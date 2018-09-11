package aQute.bnd.cdi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.component.annotations.ReferenceCardinality;

import aQute.bnd.component.MergedRequirement;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.version.Version;
import aQute.lib.strings.Strings;

/**
 * Analyze the class space for any classes that have an OSGi annotation for CCR.
 */
public class CDIAnnotations implements AnalyzerPlugin {

	public enum Discover {
		all,
		annotated,
		annotated_by_bean,
		none;

		static void parse(String s, EnumSet<Discover> options, CDIAnnotations state) {
			if (s == null)
				return;
			boolean negation = false;
			if (s.startsWith("!")) {
				negation = true;
				s = s.substring(1);
			}
			Discover option = Discover.valueOf(s);
			if (negation) {
				options.remove(option);
			} else {
				options.add(option);
			}
		}
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.CDIANNOTATIONS, "*"));
		if (header.size() == 0)
			return false;

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace()
			.values();
		List<String> names = new ArrayList<>();
		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();

		for (Clazz c : list) {
			for (Entry<Instruction, Attrs> entry : instructions.entrySet()) {
				Instruction instruction = entry.getKey();
				Attrs attrs = entry.getValue();
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}

					String discover = attrs.get("discover");
					EnumSet<Discover> options = EnumSet.noneOf(Discover.class);
					try {
						Discover.parse(discover, options, this);
					} catch (IllegalArgumentException e) {
						analyzer.error("Unrecognized discover '%s', expected values are %s", discover,
							EnumSet.allOf(Discover.class));
					}

					if (options.isEmpty()) {
						// set the default mode
						options.add(Discover.annotated_by_bean);
					}

					if (options.contains(Discover.none)) {
						break;
					}

					List<BeanDef> definitions = CDIAnnotationReader.getDefinition(c, analyzer, options);
					if (definitions == null) {
						break;
					}

					names.add(definitions.get(0).implementation.getFQN());

					if (!attrs.containsKey("noservicecapabilities")) {
						for (BeanDef beanDef : definitions) {
							if (!beanDef.service.isEmpty()) {
								int length = beanDef.service.size();
								String[] objectClass = new String[length];

								for (int i = 0; i < length; i++) {
									Descriptors.TypeRef tr = beanDef.service.get(i);
									objectClass[i] = tr.getFQN();
								}
								Arrays.sort(objectClass);
								addServiceCapability(objectClass, provides);
							}
						}
					}

					if (!attrs.containsKey("noservicerequirements")) {
						MergedRequirement serviceReqMerge = new MergedRequirement("osgi.service");
						for (ReferenceDef ref : definitions.get(0).references) {
							addServiceRequirement(ref, serviceReqMerge);
						}
						requires.addAll(serviceReqMerge.toStringList());
					}

					break;
				}
			}
		}

		if (!names.isEmpty()) {
			addExtenderRequirement(requires, names, CDIAnnotationReader.V1_0);
		}

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

	private void addExtenderRequirement(Set<String> requires, List<String> beans, Version version) {
		Version next = version.bumpMajor();
		Parameters p = new Parameters();
		Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE,
			"\"(&(osgi.extender=osgi.cdi)(version>=" + version + ")(!(version>=" + next + ")))\"");
		a.put("beans:List<String>", String.join(",", beans));
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
		return getClass().getSimpleName();
	}

}
