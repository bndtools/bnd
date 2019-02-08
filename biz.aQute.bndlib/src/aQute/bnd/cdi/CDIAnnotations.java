package aQute.bnd.cdi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import aQute.bnd.component.MergedRequirement;
import aQute.bnd.component.annotations.ReferenceCardinality;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;

/**
 * Analyze the class space for any classes that have an OSGi annotation for CCR.
 */
public class CDIAnnotations implements AnalyzerPlugin {

	static final DocumentBuilder		db;
	static final XPathExpression		discoveryModeExpression;
	static final XPathExpression		versionExpression;

	static {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);

			db = dbf.newDocumentBuilder();

			XPath xPath = XPathFactory.newInstance()
				.newXPath();

			versionExpression = xPath.compile("/beans/@version");
			discoveryModeExpression = xPath.compile("/beans/@bean-discovery-mode");
		} catch (Throwable t) {
			throw Exceptions.duck(t);
		}
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.CDIANNOTATIONS, "*"));
		if (header.size() == 0)
			return false;

		Map<Resource, Discover> discoverPerResource = new HashMap<>();
		Parameters bcp = analyzer.getBundleClassPath();
		Jar currentJar = analyzer.getJar();

		for (Entry<String, Attrs> entry : bcp.entrySet()) {
			String path = entry.getKey();
			Resource resource = currentJar.getResource(path);
			if (resource != null) {
				Jar jar = Jar.fromResource(path, resource);

				Resource beansResource = jar.getResource("META-INF/beans.xml");

				Discover discover = findDiscoveryMode(beansResource);

				jar.getResources()
					.values()
					.forEach(r -> discoverPerResource.put(r, discover));
			}
		}

		Instructions instructions = new Instructions(header);
		Packages contained = analyzer.getContained();

		List<String> names = new ArrayList<>();
		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();

		for (Clazz c : analyzer.getClassspace()
			.values()) {

			if (c.isModule() || c.isEnum() || c.isInterface() || c.isInnerClass() || c.isSynthetic()
				|| !c.is(QUERY.CONCRETE, null, analyzer)) {
				// These types cannot be managed beans so don't bother scanning
				// them. See
				// http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#what_classes_are_beans
				continue;
			}

			for (Entry<Instruction, Attrs> entry : instructions.entrySet()) {
				Instruction instruction = entry.getKey();
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}

					Attrs attrs = entry.getValue();
					String discover = attrs.get("discover");
					EnumSet<Discover> options = EnumSet.noneOf(Discover.class);
					try {
						Discover.parse(discover, options, this);
					} catch (IllegalArgumentException e) {
						analyzer.error("Unrecognized discover '%s', expected values are %s", discover,
							EnumSet.allOf(Discover.class));
					}

					if (discoverPerResource.containsKey(c.getResource())) {
						options.clear();
						Discover resourceDiscover = discoverPerResource.get(c.getResource());
						options.add(resourceDiscover);
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

	private Discover findDiscoveryMode(Resource beansResource) {
		if (beansResource == null) {
			return Discover.none;
		}

		Document document = readXMLResource(beansResource);

		if (!document.hasAttributes() && !document.hasChildNodes()) {
			return Discover.all;
		}

		try {
			Object versionResult = versionExpression.evaluate(document, XPathConstants.STRING);

			Version version = Version.valueOf(versionResult.toString());

			if ((version.equals(Version.LOWEST)) || CDIAnnotationReader.CDI_ARCHIVE_VERSION.compareTo(version) <= 0) {
				try {
					Object beanDiscoveryMode = discoveryModeExpression.evaluate(document, XPathConstants.STRING);

					return Discover.valueOf(beanDiscoveryMode.toString());
				} catch (NullPointerException | XPathExpressionException e) {
					return Discover.annotated;
				}
			}

			return Discover.all;
		} catch (NullPointerException | XPathExpressionException e) {
			return Discover.all;
		}
	}

	private Document readXMLResource(Resource resource) {
		try (InputStream is = resource.openInputStream()) {
			return db.parse(is);
		} catch (Throwable t) {
			return db.newDocument();
		}
	}

}
