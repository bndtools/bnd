package aQute.bnd.cdi;

import static java.util.stream.Collectors.toMap;

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
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.strings.Strings;
import aQute.libg.glob.PathSet;

/**
 * Analyze the class space for any classes that have an OSGi annotation for CCR.
 */
public class CDIAnnotations implements AnalyzerPlugin {
	static final DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	static final XPathFactory			xpf	= XPathFactory.newInstance();
	private static final Predicate<String>	beansResourceFilter	= new PathSet("META-INF/beans.xml").matches();

	static {
		try {
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
		} catch (Throwable t) {
			throw Exceptions.duck(t);
		}
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.CDIANNOTATIONS, "*"));
		if (header.isEmpty())
			return false;

		Jar currentJar = analyzer.getJar();

		Attrs attrs = header.get("*");
		if ((attrs != null) && !attrs.containsKey("discover")) {
			Resource beansXml = currentJar.getResource("META-INF/beans.xml");
			if (beansXml != null) {
				Discover discover = findDiscoveryMode(beansXml);
				attrs.put("discover", discover.toString());
			}
		}

		Map<String, Discover> discoverPerBCPEntry = analyzer.getBundleClassPath()
			.keySet()
			.stream()
			.filter(path -> !path.equals(".") && !path.equals("/"))
			.map(Processor::appendPath)
			.filter(path -> currentJar.exists(path) || (!path.equals("WEB-INF/classes")
				&& !path.endsWith("/WEB-INF/classes") && currentJar.hasDirectory(path)))
			.collect(toMap(path -> path, FunctionWithException.asFunction(path -> {
				Resource resource = currentJar.getResource(path);
				if (resource != null) {
					// we need to make sure to close the stream
					try (Stream<Resource> resources = Jar.getResources(resource, beansResourceFilter)) {
						Resource beansResource = resources.findFirst()
							.orElse(null);
						return findDiscoveryMode(beansResource);
					}
				} else {
					Resource beansResource = currentJar.getResource(Processor.appendPath(path, "META-INF/beans.xml"));
					return findDiscoveryMode(beansResource);
				}
			}), (u, v) -> u, HashMap::new));

		Instructions instructions = new Instructions(header);
		Packages contained = analyzer.getContained();

		List<String> names = new ArrayList<>();
		TreeSet<String> provides = new TreeSet<>();
		TreeSet<String> requires = new TreeSet<>();

		for (Clazz c : analyzer.getClassspace()
			.values()) {

			if (c.isModule() || c.isEnum() || c.isAnnotation() || c.isInnerClass() || c.isSynthetic()) {
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

					attrs = entry.getValue();
					String discover = attrs.get("discover");
					EnumSet<Discover> options = EnumSet.noneOf(Discover.class);
					try {
						Discover.parse(discover, options, this);
					} catch (IllegalArgumentException e) {
						analyzer.error("Unrecognized discover '%s', expected values are %s", discover,
							EnumSet.allOf(Discover.class));
					}

					analyzer.getBundleClassPathEntry(c)
						.map(discoverPerBCPEntry::get)
						.ifPresent(d -> {
							options.clear();
							options.add(d);
						});

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

	private Discover findDiscoveryMode(Resource beansResource) throws Exception {
		if (beansResource == null) {
			return Discover.none;
		}

		Document document = readXMLResource(beansResource);

		if (!document.hasAttributes() && !document.hasChildNodes()) {
			return Discover.all;
		}

		try {
			XPath xPath = xpf.newXPath();
			XPathExpression discoveryModeExpression = xPath.compile("/beans/@bean-discovery-mode");
			XPathExpression versionExpression = xPath.compile("/beans/@version");
			Object versionResult = versionExpression.evaluate(document, XPathConstants.STRING);

			Version version = Version.valueOf(versionResult.toString());

			if ((version.equals(Version.LOWEST)) || CDIAnnotationReader.CDI_ARCHIVE_VERSION.compareTo(version) <= 0) {
				try {
					Object beanDiscoveryMode = discoveryModeExpression.evaluate(document, XPathConstants.STRING);

					return Discover.parse(beanDiscoveryMode.toString());
				} catch (IllegalArgumentException | NullPointerException | XPathExpressionException e) {
					return Discover.annotated;
				}
			}

			return Discover.all;
		} catch (NullPointerException | XPathExpressionException e) {
			return Discover.all;
		}
	}

	private Document readXMLResource(Resource resource) throws Exception {
		DocumentBuilder db = dbf.newDocumentBuilder();
		try (InputStream is = resource.openInputStream()) {
			return db.parse(is);
		} catch (Throwable t) {
			return db.newDocument();
		}
	}

}
