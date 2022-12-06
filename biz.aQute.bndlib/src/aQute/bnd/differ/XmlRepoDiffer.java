package aQute.bnd.differ;

import static aQute.bnd.maven.MavenCapability.CAPABILITY_ARTIFACTID_ATTRIBUTE;
import static aQute.bnd.maven.MavenCapability.CAPABILITY_GROUPID_ATTRIBUTE;
import static aQute.bnd.maven.MavenCapability.MAVEN_NAMESPACE;
import static aQute.bnd.service.diff.Delta.ADDED;
import static aQute.bnd.service.diff.Delta.REMOVED;
import static aQute.bnd.service.diff.Type.ATTRIBUTE;
import static aQute.bnd.service.diff.Type.CAPABILITIES;
import static aQute.bnd.service.diff.Type.CAPABILITY;
import static aQute.bnd.service.diff.Type.DIRECTIVE;
import static aQute.bnd.service.diff.Type.EXPRESSION;
import static aQute.bnd.service.diff.Type.FILTER;
import static aQute.bnd.service.diff.Type.REPOSITORY;
import static aQute.bnd.service.diff.Type.REQUIREMENT;
import static aQute.bnd.service.diff.Type.REQUIREMENTS;
import static aQute.bnd.service.diff.Type.RESOURCE_ID;
import static aQute.bnd.service.diff.Type.VERSION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.FILTER_DIRECTIVE;
import static org.osgi.framework.namespace.AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
import static org.osgi.namespace.contract.ContractNamespace.CONTRACT_NAMESPACE;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.And;
import aQute.bnd.osgi.resource.FilterParser.ApproximateExpression;
import aQute.bnd.osgi.resource.FilterParser.BundleExpression;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.FilterParser.ExpressionVisitor;
import aQute.bnd.osgi.resource.FilterParser.HostExpression;
import aQute.bnd.osgi.resource.FilterParser.IdentityExpression;
import aQute.bnd.osgi.resource.FilterParser.Not;
import aQute.bnd.osgi.resource.FilterParser.Or;
import aQute.bnd.osgi.resource.FilterParser.PackageExpression;
import aQute.bnd.osgi.resource.FilterParser.PatternExpression;
import aQute.bnd.osgi.resource.FilterParser.RangeExpression;
import aQute.bnd.osgi.resource.FilterParser.SimpleExpression;
import aQute.bnd.osgi.resource.FilterParser.SubExpression;
import aQute.bnd.osgi.resource.FilterParser.WithRangeExpression;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.diff.Type;
import aQute.bnd.unmodifiable.Maps;
import aQute.bnd.version.Version;
import aQute.libg.tuple.Pair;

public final class XmlRepoDiffer {

	private static final String					KEY_DELIMITER					= ":";
	private static final String					ATTRIBUTE_DIRECTIVE_DELIMITER	= ":";

	// @formatter:off
	// key: namespace
	// value: attribute(s) name(s) whose associated value(s) to be used as key for comparison
	private static final Map<String, Object> COMPARATOR_ATTRIBUTES = Maps.ofEntries(
		new SimpleEntry<>(HOST_NAMESPACE, HOST_NAMESPACE),
		new SimpleEntry<>(BUNDLE_NAMESPACE, BUNDLE_NAMESPACE),
		new SimpleEntry<>(CONTENT_NAMESPACE, CONTENT_NAMESPACE),
		new SimpleEntry<>(PACKAGE_NAMESPACE, PACKAGE_NAMESPACE),
		new SimpleEntry<>(IDENTITY_NAMESPACE, IDENTITY_NAMESPACE),
		new SimpleEntry<>(CONTRACT_NAMESPACE, CONTRACT_NAMESPACE),
		new SimpleEntry<>(EXTENDER_NAMESPACE, EXTENDER_NAMESPACE),
		new SimpleEntry<>(IMPLEMENTATION_NAMESPACE, IMPLEMENTATION_NAMESPACE),
		new SimpleEntry<>(SERVICE_NAMESPACE, CAPABILITY_OBJECTCLASS_ATTRIBUTE),
		new SimpleEntry<>(EXECUTION_ENVIRONMENT_NAMESPACE, EXECUTION_ENVIRONMENT_NAMESPACE),
		new SimpleEntry<>(MAVEN_NAMESPACE, asList(CAPABILITY_GROUPID_ATTRIBUTE, CAPABILITY_ARTIFACTID_ATTRIBUTE)));
	// @formatter:on

	private XmlRepoDiffer() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	/**
	 * Returns the differ {@link Element} for comparison
	 * <p>
	 * <b>Note that,</b> the {@code filter} directives will not be expanded
	 *
	 * @param file the XML resource repository
	 * @return the differ {@link Element}
	 * @throws Exception for any discrepancy
	 * @see #resource(File, boolean)
	 */
	public static Element resource(File file) throws Exception {
		return resource(file, false);
	}

	/**
	 * Returns the differ {@link Element} for comparison
	 * <p>
	 * <b>Note that,</b> the {@code filter} directives will be expanded if
	 * {@code expandFilter} is set to {@code true}
	 *
	 * @param file the XML resource repository
	 * @param expandFilter the flag to expand {@code filter} directives
	 * @return the differ {@link Element}
	 * @throws Exception for any discrepancy
	 * @see #resource(File)
	 */
	public static Element resource(File file, boolean expandFilter) throws Exception {
		List<Element> resources = new ArrayList<>();

		List<Resource> repoResources = XMLResourceParser.getResources(file);
		if (repoResources == null) {
			throw new RuntimeException("Cannot parse the XML - " + file.getName());
		}
		// sort by resource identity
		List<Resource> sortedResources = repoResources
			.stream()
			.sorted(comparing(ResourceUtils::getIdentity))
			.collect(toList());

		for (Resource resource : sortedResources) {
			List<Element> requirementsElements = new ArrayList<>();
			List<Element> capabilitiesElements = new ArrayList<>();

			// capturing all capabilities as elements
			List<Capability> capabilities = resource.getCapabilities(null)
				.stream()
				.map(cap -> addMissingAttributes(cap, resource))
				.sorted(comparing(Capability::getNamespace))
				.collect(toList());

			for (Capability cap : capabilities) {
				Element capabilityElement = extractElement(cap.getAttributes(), cap.getDirectives(), cap.getNamespace(),
					CAPABILITY, expandFilter);
				capabilitiesElements.add(capabilityElement);
			}

			Element capabilitiesElement = new Element(CAPABILITIES, "<capabilities>", capabilitiesElements);

			// capturing all requirements as elements
			List<Requirement> requirements = resource.getRequirements(null)
				.stream()
				.map(req -> addPlaceholderAttributes(req, resource))
				.sorted(comparing(Requirement::getNamespace))
				.collect(toList());

			for (Requirement req : requirements) {
				Element requirementElement = extractElement(req.getAttributes(), req.getDirectives(),
					req.getNamespace(), REQUIREMENT, expandFilter);
				requirementsElements.add(requirementElement);
			}

			Element requirementsElement = new Element(REQUIREMENTS, "<requirements>", requirementsElements);

			// capturing resource identity as element
			String name = ResourceUtils.getIdentity(resource);

			// capturing resource version as element
			Version version = ResourceUtils.getVersion(resource);
			Element versionElement = new Element(VERSION, String.valueOf(version));

			// parent element with version, requirements and capabilities
			Element resourceElement = new Element(RESOURCE_ID, name, versionElement, requirementsElement,
				capabilitiesElement);

			resources.add(resourceElement);
		}
		return new Element(REPOSITORY, "<repository>", resources);

	}

	/**
	 * Returns the formatted comparator key for the specified attributes
	 * associating the specified namespace
	 *
	 * @param attributes the attributes
	 * @param namespace the namespace
	 * @return the formatted comparator key
	 */
	private static String formatComparatorKey(Map<String, Object> attributes, String namespace) {
		Object value = COMPARATOR_ATTRIBUTES.get(namespace);
		if (value != null) {
			if (value instanceof String) {
				Object v = attributes.get(value);
				if (v instanceof List<?> list) {
					return list.stream()
						.map(Object::toString)
						.collect(joining(KEY_DELIMITER));
				} else {
					return v.toString();
				}
			}
			if (value instanceof List<?> list) {
				return list.stream()
					.map(attributes::get)
					.map(Object::toString)
					.collect(joining(KEY_DELIMITER));
			}
		}
		return null;
	}

	/**
	 * Adds specific attributes to the specified requirement of the specified
	 * resource
	 * <p>
	 * <b>Note that,</b> the attributes are added to ease the comparison between
	 * same resources in two XML resource repositories
	 * <p>
	 * <b>Also note that,</b> the {@link #COMPARATOR_ATTRIBUTES} comprises the
	 * map of namespaces and attributes denoting which attribute of the
	 * namespace will be added as placeholders
	 *
	 * @param requirement the requirement
	 * @param resource the resource
	 * @return the updated requirement comprising the attributes as specified in
	 *         {@link #COMPARATOR_ATTRIBUTES}
	 */
	private static Requirement addPlaceholderAttributes(Requirement requirement, Resource resource) {
		CapReqBuilder capReqBuilder = CapReqBuilder.clone(requirement);
		capReqBuilder.setResource(resource);

		String namespace = requirement.getNamespace();
		FilterParser filterParser = new FilterParser();
		Expression expression = filterParser.parse(requirement);
		ComparatorKeyAttributeFinder finder = new ComparatorKeyAttributeFinder();

		Pair<String, String> attr = expression.visit(finder);

		String key = attr.getFirst();
		String value = attr.getSecond();

		Object keyNS = COMPARATOR_ATTRIBUTES.get(namespace);
		if (keyNS != null && keyNS.equals(key)) {
			capReqBuilder.addAttribute(key, value);
		}
		return capReqBuilder.buildRequirement();
	}

	/**
	 * Adds the missing attributes to the specified capability of the specified
	 * resource
	 * <p>
	 * <b>For example,</b> the {@code osgi.wiring.package} namespace must
	 * include the following attributes which are, according to specification,
	 * mandatory but {@code XMLResourceGenerator} does not add these attributes.
	 * <ul>
	 * <li>bundle-symbolic-name</li>
	 * <li>bundle-version</li>
	 * </ul>
	 *
	 * @param capability the capability
	 * @param resource the resource
	 * @return the updated capability comprising the missing attributes
	 */
	private static Capability addMissingAttributes(Capability capability, Resource resource) {
		CapReqBuilder capReqBuilder = CapReqBuilder.clone(capability);
		capReqBuilder.setResource(resource);

		String namespace = capability.getNamespace();
		Map<String, Object> attributes = capability.getAttributes();

		if (namespace.equals(PACKAGE_NAMESPACE)) {
			String bsn = ResourceUtils.getIdentity(resource);
			Version version = ResourceUtils.getVersion(resource);

			// if the bundle-symbolic-name attribute is missing, add it
			if (attributes.get(CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE) == null) {
				capReqBuilder.addAttribute(CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, bsn);
			}
			// if the bundle-version attribute is missing, add it
			if (attributes.get(CAPABILITY_BUNDLE_VERSION_ATTRIBUTE) == null) {
				capReqBuilder.addAttribute(CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version);
			}
		}
		return capReqBuilder.buildCapability();
	}

	/**
	 * Creates a single {@link Element} comprising the specified attributes and
	 * directives which are associated with the specified namespace and differ
	 * type
	 *
	 * @param attributes the attributes
	 * @param directives the directives
	 * @param namespace the namespace to associate
	 * @param type the differ type for comparison
	 * @param expandFilter the flag to expand {@code filter} directives
	 * @return the {@link Element}
	 */
	private static Element extractElement(Map<String, Object> attributes, Map<String, ? extends Object> directives,
		String namespace, Type type, boolean expandFilter) {

		List<Element> attributesElements = mapToElements(removeKeyAttribute(validate(attributes), namespace),
			ATTRIBUTE, expandFilter);
		List<Element> directivesElements = mapToElements(removeKeyAttribute(validate(directives), namespace),
			DIRECTIVE, expandFilter);

		String elementName = namespace;
		String comparatorKey = formatComparatorKey(attributes, namespace);

		if (comparatorKey != null) {
			elementName += KEY_DELIMITER + comparatorKey;
		}

		List<Element> attributesAndDirectivesElements = Stream
			.concat(attributesElements.stream(), directivesElements.stream())
			.collect(toList());

		return new Element(type, elementName, attributesAndDirectivesElements);
	}

	/**
	 * Creates list of {@link Element}s associating the specified entries
	 *
	 * @param entries the entries to associate
	 * @param type the type to use for comparison
	 * @param expandFilter the flag to expand {@code filter} directives
	 * @return the list of {@link Element}s
	 */
	public static List<Element> mapToElements(Map<String, ? extends Object> entries, Type type, boolean expandFilter) {
		List<Element> elements = new ArrayList<>();
		for (Entry<String, ? extends Object> entry : entries.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();

			String comparator;
			List<Element> children;

			// if the directive is a filter and filter expansion is enabled, we
			// create expression elements for the filter
			if (FILTER_DIRECTIVE.equals(name) && expandFilter) {
				type = FILTER;
				comparator = "<filter>";
				children = createFilterElement(value.toString());
			} else {
				comparator = name + ATTRIBUTE_DIRECTIVE_DELIMITER + value;
				children = null;
			}
			Element element = new Element(type, comparator, children, ADDED, REMOVED, null);
			elements.add(element);
		}
		return elements;
	}

	/**
	 * Iterates over a map of entries and if any of entry has a value which is
	 * of type {@link List}, new entries get created for every element
	 * containing in the value. This is required for attributes and directive of
	 * type {@link List}
	 *
	 * @param entries the map of entries (attributes or directives)
	 * @return the new map of entries
	 */
	private static Map<String, ? extends Object> validate(Map<String, ? extends Object> entries) {
		Map<String, ? super Object> finalEntries = new HashMap<>();

		for (Entry<String, ? extends Object> entry : entries.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();

			if (val instanceof List<?> list) {
				// expanding attribute or directive of type 'List'
				list.forEach(v -> finalEntries.put(key, v));
			} else {
				finalEntries.put(key, val);
			}
		}
		return finalEntries;
	}

	/**
	 * Removes the attribute from the attributes list which has been used as
	 * comparator key for comparison.
	 *
	 * @param attributes the attributes from which the key attribute is removed
	 * @param namespace the namespace to check for the key attribute name
	 * @return the final map of attributes without the key attribute
	 */
	private static Map<String, ? extends Object> removeKeyAttribute(Map<String, ? extends Object> attributes,
		String namespace) {
		Object value = COMPARATOR_ATTRIBUTES.get(namespace);
		if (value == null) {
			return attributes;
		}
		if (value instanceof String string) {
			attributes.remove(string);
		} else if (value instanceof List<?> list) {
			list.stream()
				.map(Object::toString)
				.forEach(attributes::remove);
		}
		return attributes;
	}

	/**
	 * Creates list of {@link Element}s for the associated filter
	 *
	 * @param filter the filter
	 * @return the list of {@link Element}
	 */
	private static List<Element> createFilterElement(String filter) {
		Expression expression = new FilterParser().parse(filter);
		Map<String, String> attrs = expression.visit(new FilterVisitor());
		return mapToElements(attrs, EXPRESSION, false);
	}

	/**
	 * Used to find the comparator key attribute in the {@code filter} directive
	 */
	private static class ComparatorKeyAttributeFinder extends ExpressionVisitor<Pair<String, String>> {

		public ComparatorKeyAttributeFinder() {
			super(null);
		}

		@Override
		public Pair<String, String> visit(SimpleExpression expr) {
			return Pair.newInstance(expr.getKey(), expr.getValue());
		}

		@Override
		public Pair<String, String> visit(PackageExpression expr) {
			return Pair.newInstance(PACKAGE_NAMESPACE, expr.getPackageName());
		}

		@Override
		public Pair<String, String> visit(BundleExpression expr) {
			return Pair.newInstance(BUNDLE_NAMESPACE, expr.printExcludingRange());
		}

		@Override
		public Pair<String, String> visit(HostExpression expr) {
			return Pair.newInstance(HOST_NAMESPACE, expr.getHostName());
		}

		@Override
		public Pair<String, String> visit(IdentityExpression expr) {
			return Pair.newInstance(IDENTITY_NAMESPACE, expr.getSymbolicName());
		}

		@Override
		public Pair<String, String> visit(PatternExpression expr) {
			return Pair.newInstance(expr.getKey(), expr.getValue());
		}

		@Override
		public Pair<String, String> visit(And expr) {
			return visit((SubExpression) expr);
		}

		@Override
		public Pair<String, String> visit(Or expr) {
			return visit((SubExpression) expr);
		}

		@Override
		public Pair<String, String> visit(ApproximateExpression expr) {
			return visit((SimpleExpression) expr);
		}

		@Override
		public Pair<String, String> visit(RangeExpression expr) {
			return visit((SimpleExpression) expr);
		}

		private Pair<String, String> visit(SubExpression expr) {
			for (Expression ex : expr.getExpressions()) {
				if (ex instanceof And || ex instanceof Or) {
					return visit((SubExpression) ex);
				}
				if (ex instanceof SimpleExpression simpleExpression) {
					return visit(simpleExpression);
				}
			}
			return Pair.newInstance(null, null);
		}

	}

	/**
	 * Used to prepare a map containing relevant informations from a
	 * {@code filter} directive
	 */
	private static class FilterVisitor extends ExpressionVisitor<Map<String, String>> {

		private final Map<String, String> entries = new HashMap<>();

		public FilterVisitor() {
			super(emptyMap());
		}

		@Override
		public Map<String, String> visit(SimpleExpression expr) {
			entries.computeIfAbsent(expr.getKey(), v -> expr.getValue());
			entries.computeIfAbsent("op", v -> Optional.ofNullable(expr.getOp())
				.map(Object::toString)
				.orElse(null));
			return entries;
		}

		@Override
		public Map<String, String> visit(BundleExpression expr) {
			return visit(expr, "bsn");
		}

		@Override
		public Map<String, String> visit(HostExpression expr) {
			return visit(expr, "host");
		}

		@Override
		public Map<String, String> visit(PackageExpression expr) {
			return visit(expr, "package");
		}

		@Override
		public Map<String, String> visit(IdentityExpression expr) {
			return visit(expr, "bsn");
		}

		@Override
		public Map<String, String> visit(PatternExpression expr) {
			return visit((SimpleExpression) expr);
		}

		@Override
		public Map<String, String> visit(ApproximateExpression expr) {
			return visit((SimpleExpression) expr);
		}

		@Override
		public Map<String, String> visit(RangeExpression expr) {
			visit((SimpleExpression) expr);
			entries.computeIfAbsent("range", v -> expr.getRangeString());
			return entries;
		}

		@Override
		public Map<String, String> visit(And expr) {
			return visit((SubExpression) expr);
		}

		@Override
		public Map<String, String> visit(Or expr) {
			return visit((SubExpression) expr);
		}

		@Override
		public Map<String, String> visit(Not expr) {
			entries.computeIfAbsent("query", v -> expr.query());
			return entries;
		}

		private Map<String, String> visit(WithRangeExpression expr, String key) {
			entries.computeIfAbsent(key, v -> expr.printExcludingRange());
			entries.computeIfAbsent("range", v -> Optional.ofNullable(expr.getRangeExpression())
				.map(Object::toString)
				.orElse(null));
			return entries;
		}

		private Map<String, String> visit(SubExpression expr) {
			FilterVisitor visitor = new FilterVisitor();
			for (Expression ex : expr.getExpressions()) {
				entries.putAll(ex.visit(visitor));
			}
			return entries;
		}

	}

}
