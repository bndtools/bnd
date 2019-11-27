package aQute.bnd.osgi.resource;

import static aQute.lib.exceptions.BiConsumerWithException.asBiConsumer;
import static aQute.lib.exceptions.BiFunctionWithException.asBiFunction;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.VersionRange;
import aQute.lib.converter.Converter;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;

public class CapReqBuilder {

	private static final String			REQ_ALIAS_IDENTITY					= "bnd.identity";
	private static final String			REQ_ALIAS_IDENTITY_NAME_ATTRIB		= "id";
	private static final String			REQ_ALIAS_IDENTITY_VERSION_ATTRIB	= "version";

	private static final String			REQ_ALIAS_LITERAL					= "bnd.literal";
	private static final String			REQ_ALIAS_LITERAL_ATTRIB			= REQ_ALIAS_LITERAL;

	private final String				namespace;
	private Resource					resource;
	private final Map<String, Object>	attributes							= new HashMap<>();
	private final Map<String, String>	directives							= new HashMap<>();

	public CapReqBuilder(String namespace) {
		this.namespace = requireNonNull(namespace);
	}

	public CapReqBuilder(String namespace, Attrs attrs) throws Exception {
		this(namespace);
		addAttributesOrDirectives(attrs);
	}

	public CapReqBuilder(Resource resource, String namespace) {
		this(namespace);
		setResource(resource);
	}

	public static CapReqBuilder clone(Capability capability) throws Exception {
		CapabilityBuilder builder = new CapabilityBuilder(capability.getNamespace());
		builder.addAttributes(capability.getAttributes());
		builder.addDirectives(capability.getDirectives());
		return builder;
	}

	public static CapReqBuilder clone(Requirement requirement) throws Exception {
		RequirementBuilder builder = new RequirementBuilder(requirement.getNamespace());
		builder.addAttributes(requirement.getAttributes());
		builder.addDirectives(requirement.getDirectives());
		return builder;
	}

	public String getNamespace() {
		return namespace;
	}

	public Resource getResource() {
		return resource;
	}

	public CapReqBuilder setResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public CapReqBuilder addAttribute(String name, Object value) throws Exception {
		if (value == null)
			return this;

		if (value.getClass()
			.isArray()) {
			value = Converter.cnv(List.class, value);
		}

		if (name.equals(ResourceUtils.getVersionAttributeForNamespace(namespace))) {
			value = toVersions(value);
		}

		attributes.put(name, value);
		return this;
	}

	public boolean isVersion(Object value) {
		if (value instanceof Version)
			return true;

		if (value instanceof Collection) {
			if (((Collection<?>) value).isEmpty())
				return true;

			return isVersion(((Collection<?>) value).iterator()
				.next());
		}
		if (value.getClass()
			.isArray()) {
			if (Array.getLength(value) == 0)
				return true;

			return isVersion(((Object[]) value)[0]);
		}
		return false;
	}

	public CapReqBuilder addAttributes(Map<? extends String, ? extends Object> attributes) throws Exception {
		MapStream.of(attributes)
			.forEachOrdered(asBiConsumer(this::addAttribute));
		return this;
	}

	public CapReqBuilder addDirective(String name, String value) {
		if (value == null)
			return this;

		directives.put(ResourceUtils.stripDirective(name), value);
		return this;
	}

	public CapReqBuilder addDirectives(Attrs directives) {
		directives.stream()
			.mapKey(Attrs::toDirective)
			.filterKey(Objects::nonNull)
			.forEachOrdered(this::addDirective);
		return this;
	}

	public CapReqBuilder addDirectives(Map<String, String> directives) {
		MapStream.of(directives)
			.forEachOrdered(this::addDirective);
		return this;
	}

	public Capability buildCapability() {
		if (resource == null)
			throw new IllegalStateException("Cannot build Capability with null Resource.");
		return new CapabilityImpl(namespace, resource, directives, attributes);
	}

	public Capability buildSyntheticCapability() {
		return new CapabilityImpl(namespace, resource, directives, attributes);
	}

	public Requirement buildRequirement() {
		if (resource == null)
			throw new IllegalStateException(
				"Cannot build Requirement with null Resource. use buildSyntheticRequirement");
		return new RequirementImpl(namespace, resource, directives, attributes);
	}

	public Requirement buildSyntheticRequirement() {
		return new RequirementImpl(namespace, null, directives, attributes);
	}

	public static final CapReqBuilder createPackageRequirement(String pkgName, String range) {
		Filter filter;
		SimpleFilter pkgNameFilter = new SimpleFilter(PackageNamespace.PACKAGE_NAMESPACE, pkgName);
		if (range != null)
			filter = new AndFilter().addChild(pkgNameFilter)
				.addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = pkgNameFilter;

		return new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
	}

	public static CapReqBuilder createBundleRequirement(String bsn, String range) {
		Filter filter;
		SimpleFilter bsnFilter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
		if (range != null)
			filter = new AndFilter().addChild(bsnFilter)
				.addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = bsnFilter;

		return new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

	}

	public static CapReqBuilder createSimpleRequirement(String ns, String name, String range) {
		Filter filter;
		SimpleFilter bsnFilter = new SimpleFilter(ns, name);
		if (range != null)
			filter = new AndFilter().addChild(bsnFilter)
				.addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = bsnFilter;

		return new CapReqBuilder(ns).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

	}

	public CharSequence and(Object... exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append("(&");
		for (Object expr : exprs) {
			sb.append("(")
				.append(toFilter(expr))
				.append(")");
		}
		sb.append(")");
		return sb;
	}

	public CharSequence or(Object... exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append("(|");
		for (Object expr : exprs) {
			sb.append("(")
				.append(toFilter(expr))
				.append(")");
		}
		sb.append(")");
		return sb;
	}

	public CharSequence not(Object expr) {
		StringBuilder sb = new StringBuilder();
		sb.append("(!(")
			.append(toFilter(expr))
			.append(")");
		return sb;
	}

	private CharSequence toFilter(Object expr) {
		if (expr instanceof CharSequence)
			return (CharSequence) expr;

		if (expr instanceof Filter) {
			return expr.toString();
		}

		if (expr instanceof VersionRange) {
			return ((VersionRange) expr).toFilter();
		}

		return expr.toString();
	}

	public CapReqBuilder filter(CharSequence f) {
		return addDirective("filter", f.toString());
	}

	/**
	 * Equivalent to {@code getRequirementsFrom(rr, true)}.
	 *
	 * @param rr
	 */
	public static List<Requirement> getRequirementsFrom(Parameters rr) throws Exception {
		return getRequirementsFrom(rr, true);
	}

	/**
	 * Parse requirements from a Parameters set in the form of an OSGi
	 * Require-Capability header.
	 *
	 * @param rr The Require-Capability header.
	 * @param unalias Whether to unalias requirements. If false then an aliases
	 *            such as "bundle; bsn=org.foo" will be returned as a raw
	 *            Requirement in the unspecified namespace "bundle".
	 * @return The list of parsed requirements.
	 * @throws Exception
	 */
	public static List<Requirement> getRequirementsFrom(Parameters rr, boolean unalias) throws Exception {
		List<Requirement> requirements = rr.stream()
			.mapToObj(asBiFunction((k, v) -> getRequirementFrom(Processor.removeDuplicateMarker(k), v, unalias)))
			.collect(toList());
		return requirements;
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs) throws Exception {
		return getRequirementFrom(namespace, attrs, true);
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs, boolean unalias) throws Exception {
		CapReqBuilder builder = createCapReqBuilder(namespace, attrs);
		Requirement requirement = builder.buildSyntheticRequirement();
		if (unalias)
			requirement = unalias(requirement);
		return requirement;
	}

	public static CapReqBuilder createCapReqBuilder(String namespace, Attrs attrs) throws Exception {
		CapReqBuilder builder = new CapReqBuilder(namespace);
		for (String key : attrs.keySet()) {
			if (key.endsWith(":")) {
				String value = attrs.get(key);
				key = key.substring(0, key.length() - 1);
				builder.addDirective(key, value);
			} else {
				Object value = attrs.getTyped(key);
				builder.addAttribute(key, value);
			}
		}
		return builder;
	}

	/**
	 * Convert an alias requirement to its canonical form. For example:
	 * "<code>bnd.identity; id=org.example; version='[1.0,2.0)'</code>" will be
	 * converted to
	 * "<code>osgi.identity; filter:='(&(osgi.identity=org.example)(version>=1.0)(!(version>=2.0)))'</code>"
	 * Requirements that are not recognized as aliases will be returned
	 * unchanged.
	 */
	public static Requirement unalias(Requirement requirement) throws Exception {
		if (requirement == null)
			return null;

		final String ns = requirement.getNamespace();

		final Set<String> consumedAttribs = new HashSet<>();
		final Set<String> consumedDirectives = new HashSet<>();

		if (REQ_ALIAS_LITERAL.equals(ns)) {
			final String literalNs = Objects.toString(requirement.getAttributes()
				.get(REQ_ALIAS_LITERAL_ATTRIB), null);
			consumedAttribs.add(REQ_ALIAS_LITERAL_ATTRIB);
			if (literalNs == null) {
				throw new IllegalArgumentException(
					String.format("Requirement alias %s is missing mandatory attribute '%s' of type String",
						REQ_ALIAS_LITERAL, REQ_ALIAS_LITERAL_ATTRIB));
			}

			CapReqBuilder builder = new CapReqBuilder(literalNs);
			copyAttribs(requirement, builder, consumedAttribs);
			copyDirectives(requirement, builder, Collections.emptySet());
			requirement = builder.buildSyntheticRequirement();
		} else if (REQ_ALIAS_IDENTITY.equals(ns)) {
			final String bsn = Objects.toString(requirement.getAttributes()
				.get(REQ_ALIAS_IDENTITY_NAME_ATTRIB), null);
			consumedAttribs.add(REQ_ALIAS_IDENTITY_NAME_ATTRIB);
			if (bsn == null) {
				throw new IllegalArgumentException(
					String.format("Requirement alias '%s' is missing mandatory attribute '%s' of type String",
						REQ_ALIAS_IDENTITY, REQ_ALIAS_IDENTITY_NAME_ATTRIB));
			}

			final VersionRange range = toRange(requirement.getAttributes()
				.get(REQ_ALIAS_IDENTITY_VERSION_ATTRIB));
			consumedAttribs.add(REQ_ALIAS_IDENTITY_VERSION_ATTRIB);
			CapReqBuilder b = CapReqBuilder.createBundleRequirement(bsn, Objects.toString(range, null));
			copyAttribs(requirement, b, consumedAttribs);
			copyDirectives(requirement, b, consumedDirectives);
			requirement = b.buildSyntheticRequirement();
		}

		return requirement;
	}

	private static void copyAttribs(Requirement req, CapReqBuilder builder, Set<String> excludes) throws Exception {
		MapStream.of(req.getAttributes())
			.filterKey(key -> !excludes.contains(key))
			.forEachOrdered(asBiConsumer(builder::addAttribute));
	}

	private static void copyDirectives(Requirement req, CapReqBuilder builder, Set<String> excludes) throws Exception {
		MapStream.of(req.getDirectives())
			.filterKey(key -> !excludes.contains(key))
			.forEachOrdered(builder::addDirective);
	}

	private static VersionRange toRange(Object o) throws IllegalArgumentException {
		final VersionRange range;
		if (o == null)
			range = null;
		else if (o instanceof VersionRange)
			range = (VersionRange) o;
		else if (o instanceof org.osgi.framework.VersionRange || o instanceof Version || o instanceof String)
			range = VersionRange.parseOSGiVersionRange(o.toString());
		else
			throw new IllegalArgumentException("Expected type String, Version or VersionRange");
		return range;
	}

	public static List<Capability> getCapabilitiesFrom(Parameters rr) throws Exception {
		List<Capability> capabilities = rr.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.mapToObj(asBiFunction(CapReqBuilder::getCapabilityFrom))
			.collect(toList());
		return capabilities;
	}

	public static Capability getCapabilityFrom(String namespace, Attrs attrs) throws Exception {
		CapReqBuilder builder = createCapReqBuilder(namespace, attrs);
		return builder.buildSyntheticCapability();
	}

	public CapReqBuilder from(Capability c) throws Exception {
		addAttributes(c.getAttributes());
		addDirectives(c.getDirectives());
		return this;
	}

	public CapReqBuilder from(Requirement r) throws Exception {
		addAttributes(r.getAttributes());
		addDirectives(r.getDirectives());
		return this;
	}

	public static Capability copy(Capability c, Resource r) throws Exception {
		CapReqBuilder from = new CapReqBuilder(c.getNamespace()).from(c);
		if (r == null)
			return from.buildSyntheticCapability();
		else
			return from.setResource(r)
				.buildCapability();
	}

	public static Requirement copy(Requirement c, Resource r) throws Exception {
		CapReqBuilder from = new CapReqBuilder(c.getNamespace()).from(c);
		if (r == null)
			return from.buildSyntheticRequirement();
		else
			return from.setResource(r)
				.buildRequirement();
	}

	/**
	 * In bnd, we only use one map for both directives & attributes. This method
	 * will properly dispatch them AND take care of typing
	 *
	 * @param attrs
	 * @throws Exception
	 */
	public void addAttributesOrDirectives(Attrs attrs) throws Exception {
		for (Entry<String, String> e : attrs.entrySet()) {
			String directive = Attrs.toDirective(e.getKey());
			if (directive != null) {
				addDirective(directive, e.getValue());
			} else {
				Object typed = attrs.getTyped(e.getKey());
				if (typed instanceof aQute.bnd.version.Version) {
					typed = new Version(typed.toString());
				}
				addAttribute(e.getKey(), typed);
			}
		}

	}

	public void addFilter(String ns, String name, String version, Attrs attrs) {
		List<String> parts = new ArrayList<>();

		parts.add("(" + ns + "=" + name + ")");
		if (version != null && VersionRange.isOSGiVersionRange(version)) {
			VersionRange range = VersionRange.parseOSGiVersionRange(version);
			parts.add(range.toFilter());
		}

		String mandatory = attrs.get(Constants.MANDATORY_DIRECTIVE + ":");
		if (mandatory != null) {
			String mandatoryAttrs[] = mandatory.split("\\s*,\\s*");
			Arrays.sort(mandatoryAttrs);
			for (String mandatoryAttr : mandatoryAttrs) {
				String value = attrs.get(mandatoryAttr);
				if (value != null) {
					parts.add("(" + mandatoryAttr + "=" + escapeFilterValue(value) + ")");
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		if (parts.size() > 0)
			sb.append("(&");
		for (String s : parts) {
			sb.append(s);
		}
		if (parts.size() > 0)
			sb.append(")");
		addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, sb.toString());
	}

	/**
	 * If value must contain one of the characters reverse solidus ('\' \u005C),
	 * asterisk ('*' \u002A), parentheses open ('(' \u0028) or parentheses close
	 * (')' \u0029), then these characters should be preceded with the reverse
	 * solidus ('\' \u005C) character. Spaces are significant in value. Space
	 * characters are defined by Character.isWhiteSpace().
	 */

	private final static Pattern ESCAPE_FILTER_VALUE_P = Pattern.compile("[\\\\*()]");

	public static String escapeFilterValue(String value) {
		return ESCAPE_FILTER_VALUE_P.matcher(value)
			.replaceAll("\\\\$0");
	}

	public void and(String... s) {
		String previous = directives == null ? null : directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		StringBuilder filter = new StringBuilder();

		if (previous != null) {
			filter.append("(&")
				.append(previous);
		}
		for (String subexpr : s)
			filter.append(subexpr);

		if (previous != null) {
			filter.append(")");
		}
		addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
	}

	public boolean isPackage() {
		return PackageNamespace.PACKAGE_NAMESPACE.equals(getNamespace());
	}

	public boolean isHost() {
		return HostNamespace.HOST_NAMESPACE.equals(getNamespace());
	}

	public boolean isBundle() {
		return BundleNamespace.BUNDLE_NAMESPACE.equals(getNamespace());
	}

	public boolean isService() {
		return ServiceNamespace.SERVICE_NAMESPACE.equals(getNamespace());
	}

	public boolean isContract() {
		return ContractNamespace.CONTRACT_NAMESPACE.equals(getNamespace());
	}

	public boolean isIdentity() {
		return IdentityNamespace.IDENTITY_NAMESPACE.equals(getNamespace());
	}

	public boolean isContent() {
		return ContentNamespace.CONTENT_NAMESPACE.equals(getNamespace());
	}

	public boolean isEE() {
		return ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(getNamespace());
	}

	public boolean isExtender() {
		return ExtenderNamespace.EXTENDER_NAMESPACE.equals(getNamespace());
	}

	public Attrs toAttrs() {
		Attrs attrs = new Attrs();

		MapStream.ofNullable(attributes)
			.map((key, value) -> (key.equals("version") || (value instanceof Version))
				? MapStream.entry(key, toBndVersions(value))
				: MapStream.entry(key, value))
			.forEachOrdered(attrs::putTyped);

		MapStream.ofNullable(directives)
			.mapKey(key -> key.concat(":"))
			.forEachOrdered(attrs::put);

		return attrs;
	}

	private Object toBndVersions(Object value) {
		if (value instanceof aQute.bnd.version.Version)
			return value;

		if (value instanceof Version)
			return new aQute.bnd.version.Version(value.toString());

		if (value instanceof String)
			return new aQute.bnd.version.Version((String) value);

		if (value instanceof Collection) {
			List<aQute.bnd.version.Version> bnds = new ArrayList<>();
			for (Object m : (Collection<?>) value) {
				bnds.add((aQute.bnd.version.Version) toBndVersions(m));
			}
			return bnds;
		}

		throw new IllegalArgumentException("cannot convert " + value + " to a bnd Version(s) object as requested");
	}

	private Object toVersions(Object value) {
		if (value instanceof Version)
			return value;

		if (value instanceof aQute.bnd.version.Version)
			return new Version(value.toString());

		if (value instanceof String)
			try {
				return new Version((String) value);
			} catch (Exception e) {
				return value;
			}

		if (value instanceof Number)
			try {
				return new Version(((Number) value).intValue(), 0, 0);
			} catch (Exception e) {
				return value;
			}

		if (value instanceof Collection) {
			Collection<?> v = (Collection<?>) value;
			if (v.isEmpty())
				return value;

			if (v.iterator()
				.next() instanceof Version)
				return value;

			List<Version> osgis = new ArrayList<>();
			for (Object m : (Collection<?>) value) {
				osgis.add((Version) toVersions(m));
			}
			return osgis;
		}

		throw new IllegalArgumentException(
			"cannot convert " + value + " to a org.osgi.framework Version(s) object as requested");
	}

	public static RequirementBuilder createRequirementFromCapability(Capability cap) {
		RequirementBuilder req = new RequirementBuilder(cap.getNamespace());
		StringBuilder sb = new StringBuilder("(&");
		for (Entry<String, Object> e : cap.getAttributes()
			.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Version || e.getKey()
				.equals("version")) {
				VersionRange r = new VersionRange(v.toString());
				String filter = r.toFilter();
				sb.append(filter);

			} else
				sb.append("(")
					.append(e.getKey())
					.append("=")
					.append(escapeFilterValue((String) v))
					.append(")");
		}
		sb.append(")");

		req.and(sb.toString());
		return req;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[')
			.append(namespace)
			.append('[');
		sb.append(attributes);
		sb.append(directives);
		return sb.toString();
	}
}
