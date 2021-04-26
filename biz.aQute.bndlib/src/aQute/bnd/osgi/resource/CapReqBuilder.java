package aQute.bnd.osgi.resource;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
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
import aQute.lib.exceptions.Exceptions;

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

	public CapReqBuilder(String namespace, Attrs attrs) {
		this(namespace);
		addAttributesOrDirectives(attrs);
	}

	public CapReqBuilder(Resource resource, String namespace) {
		this(namespace);
		setResource(resource);
	}

	public static CapReqBuilder clone(Capability capability) {
		return new CapReqBuilder(capability.getNamespace()).from(capability);
	}

	public static CapReqBuilder clone(Requirement requirement) {
		return new CapReqBuilder(requirement.getNamespace()).from(requirement);
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

	public CapReqBuilder addAttribute(String name, Object value) {
		if (value == null)
			return this;

		if (value.getClass()
			.isArray()) {
			try {
				value = Converter.cnv(List.class, value);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		if (isVersion(value, aQute.bnd.version.Version.class)
			|| name.equals(ResourceUtils.getVersionAttributeForNamespace(getNamespace()))) {
			value = toVersions(value);
		}

		attributes.put(name, value);
		return this;
	}

	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}

	public CapReqBuilder removeAttribute(String name) {
		attributes.remove(name);
		return this;
	}

	public boolean isVersion(Object value) {
		return isVersion(value, Version.class);
	}

	private boolean isVersion(Object value, Class<?> versionClass) {
		if (versionClass.isInstance(value)) {
			return true;
		}
		if (value instanceof Collection) {
			Collection<?> v = (Collection<?>) value;
			if (v.isEmpty()) {
				return false;
			}
			return isVersion(v.iterator()
				.next(), versionClass);
		}
		if (value.getClass()
			.isArray()) {
			if (versionClass.isAssignableFrom(value.getClass()
				.getComponentType())) {
				return true;
			}
			if (Array.getLength(value) == 0) {
				return false;
			}

			return isVersion(Array.get(value, 0), versionClass);
		}
		return false;
	}

	public CapReqBuilder addAttributes(Map<? extends String, ? extends Object> attributes) {
		attributes.forEach(this::addAttribute);
		return this;
	}

	public CapReqBuilder addDirective(String name, String value) {
		if (value == null)
			return this;

		directives.put(ResourceUtils.stripDirective(name), value);
		return this;
	}

	public boolean hasDirective(String name) {
		return directives.containsKey(ResourceUtils.stripDirective(name));
	}

	public CapReqBuilder removeDirective(String name) {
		directives.remove(ResourceUtils.stripDirective(name));
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
		directives.forEach(this::addDirective);
		return this;
	}

	public Capability buildCapability() {
		if (resource == null)
			throw new IllegalStateException("Cannot build Capability with null Resource.");
		return new CapabilityImpl(namespace, resource, directives, attributes);
	}

	public Capability buildSyntheticCapability() {
		return new CapabilityImpl(namespace, null, directives, attributes);
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

	public static CapReqBuilder createPackageRequirement(String name, String versionRange) {
		return createPackageRequirement(name, null, versionRange);
	}

	public static RequirementBuilder createPackageRequirement(String name, Attrs attrs, String versionRange) {
		RequirementBuilder builder = new RequirementBuilder(PackageNamespace.PACKAGE_NAMESPACE);
		if (attrs != null) {
			builder.addDirectives(attrs);
			if (versionRange == null) {
				versionRange = attrs.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			}
		}
		builder.addFilter(PackageNamespace.PACKAGE_NAMESPACE, name, versionRange, attrs);
		// Add package name as an attribute to help other parts of Bnd find the
		// name of the required package without having to grovel through the
		// filter string.
		builder.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, name);
		return builder;
	}

	public static CapabilityBuilder createPackageCapability(String name, Attrs attrs, String bundle_symbolic_name,
		Version bundle_version) {
		CapabilityBuilder builder = new CapabilityBuilder(PackageNamespace.PACKAGE_NAMESPACE);
		if (attrs != null) {
			builder.addAttributesOrDirectives(attrs);
		}
		if (!builder.hasAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE)) {
			builder.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion);
		}
		builder.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, requireNonNull(name));
		if (bundle_symbolic_name != null) {
			builder.addAttribute(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundle_symbolic_name);
		}
		if (bundle_version != null) {
			builder.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, bundle_version);
		}
		return builder;
	}

	public static CapReqBuilder createBundleRequirement(String name, String versionRange) {
		return createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, name, versionRange);
	}

	public static CapReqBuilder createSimpleRequirement(String namespace, String name, String versionRange) {
		RequirementBuilder builder = new RequirementBuilder(namespace);
		builder.addFilter(namespace, name, versionRange, null);
		return builder;
	}

	public static RequirementBuilder createServiceRequirement(String name) {
		RequirementBuilder builder = new RequirementBuilder(ServiceNamespace.SERVICE_NAMESPACE);
		builder.addFilter(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, name, null, null);
		return builder;
	}

	public CharSequence and(Object... exprs) {
		StringBuilder sb = new StringBuilder().append('(')
			.append('&');
		for (Object expr : exprs) {
			sb.append(toFilter(expr));
		}
		return sb.append(')');
	}

	public CharSequence or(Object... exprs) {
		StringBuilder sb = new StringBuilder().append('(')
			.append('|');
		for (Object expr : exprs) {
			sb.append(toFilter(expr));
		}
		return sb.append(')');
	}

	public CharSequence not(Object expr) {
		StringBuilder sb = new StringBuilder().append('(')
			.append('!');
		sb.append(toFilter(expr));
		return sb.append(')');
	}

	private CharSequence toFilter(Object expr) {
		if (expr instanceof CharSequence)
			return (CharSequence) expr;

		if (expr instanceof VersionRange) {
			return ((VersionRange) expr).toFilter();
		}

		return expr.toString();
	}

	public CapReqBuilder filter(CharSequence f) {
		return addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, f.toString());
	}

	/**
	 * Equivalent to {@code getRequirementsFrom(rr, true)}.
	 *
	 * @param rr
	 */
	public static List<Requirement> getRequirementsFrom(Parameters rr) {
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
	 */
	public static List<Requirement> getRequirementsFrom(Parameters rr, boolean unalias) {
		List<Requirement> requirements = rr.stream()
			.mapToObj((k, v) -> getRequirementFrom(Processor.removeDuplicateMarker(k), v, unalias))
			.collect(toList());
		return requirements;
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs) {
		return getRequirementFrom(namespace, attrs, true);
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs, boolean unalias) {
		CapReqBuilder builder = createCapReqBuilder(namespace, attrs);
		Requirement requirement = builder.buildSyntheticRequirement();
		if (unalias)
			requirement = unalias(requirement);
		return requirement;
	}

	public static CapReqBuilder createCapReqBuilder(String namespace, Attrs attrs) {
		CapReqBuilder builder = new CapReqBuilder(namespace);
		if (attrs != null) {
			builder.addAttributesOrDirectives(attrs);
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
	public static Requirement unalias(Requirement requirement) {
		if (requirement == null) {
			return null;
		}
		switch (requirement.getNamespace()) {
			case REQ_ALIAS_LITERAL : {
				String namespace = Objects.toString(requirement.getAttributes()
					.get(REQ_ALIAS_LITERAL_ATTRIB), null);
				if (namespace == null) {
					throw new IllegalArgumentException(
						String.format("Requirement alias %s is missing mandatory attribute '%s' of type String",
							REQ_ALIAS_LITERAL, REQ_ALIAS_LITERAL_ATTRIB));
				}
				CapReqBuilder builder = new CapReqBuilder(namespace).from(requirement)
					.removeAttribute(REQ_ALIAS_LITERAL_ATTRIB);
				return builder.buildSyntheticRequirement();
			}
			case REQ_ALIAS_IDENTITY : {
				String name = Objects.toString(requirement.getAttributes()
					.get(REQ_ALIAS_IDENTITY_NAME_ATTRIB), null);
				String versionRange = Objects.toString(requirement.getAttributes()
					.get(REQ_ALIAS_IDENTITY_VERSION_ATTRIB), null);
				if (name == null) {
					throw new IllegalArgumentException(
						String.format("Requirement alias '%s' is missing mandatory attribute '%s' of type String",
							REQ_ALIAS_IDENTITY, REQ_ALIAS_IDENTITY_NAME_ATTRIB));
				}
				CapReqBuilder builder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).from(requirement)
					.removeAttribute(REQ_ALIAS_IDENTITY_NAME_ATTRIB)
					.removeAttribute(REQ_ALIAS_IDENTITY_VERSION_ATTRIB);
				builder.addFilter(IdentityNamespace.IDENTITY_NAMESPACE, name, versionRange, null);
				return builder.buildSyntheticRequirement();
			}
			default : {
				return requirement;
			}
		}
	}

	public static List<Capability> getCapabilitiesFrom(Parameters rr) {
		List<Capability> capabilities = rr.stream()
			.mapToObj((k, v) -> getCapabilityFrom(Processor.removeDuplicateMarker(k), v))
			.collect(toList());
		return capabilities;
	}

	public static Capability getCapabilityFrom(String namespace, Attrs attrs) {
		return createCapReqBuilder(namespace, attrs).buildSyntheticCapability();
	}

	public CapReqBuilder from(Capability capability) {
		return addAttributes(capability.getAttributes()).addDirectives(capability.getDirectives());
	}

	public CapReqBuilder from(Requirement requirement) {
		return addAttributes(requirement.getAttributes()).addDirectives(requirement.getDirectives());
	}

	public static Capability copy(Capability capability, Resource resource) {
		CapReqBuilder clone = clone(capability);
		return (resource != null) ? clone.setResource(resource)
			.buildCapability() : clone.buildSyntheticCapability();
	}

	public static Requirement copy(Requirement requirement, Resource resource) {
		CapReqBuilder clone = clone(requirement);
		return (resource != null) ? clone.setResource(resource)
			.buildRequirement() : clone.buildSyntheticRequirement();
	}

	/**
	 * In bnd, we use one map for both directives & attributes. This method will
	 * properly dispatch them AND take care of typing
	 *
	 * @param attrs
	 */
	public void addAttributesOrDirectives(Attrs attrs) {
		for (Entry<String, String> e : attrs.entrySet()) {
			String name = e.getKey();
			String directive = Attrs.toDirective(name);
			if (directive != null) {
				addDirective(directive, e.getValue());
			} else {
				Object typed = attrs.getTyped(name);
				addAttribute(name, typed);
			}
		}
	}

	/**
	 * In bnd, we use one map for both directives & attributes. This method will
	 * ignore directives.
	 *
	 * @param attrs
	 */
	public CapReqBuilder addAttributes(Attrs attrs) {
		attrs.keySet()
			.stream()
			.filter(Attrs::isAttribute)
			.forEachOrdered(name -> {
				Object typed = attrs.getTyped(name);
				addAttribute(name, typed);
			});
		return this;
	}

	public void addFilter(String nameAttr, String name, String versionRange, Attrs attrs) {
		StringBuilder filter = new StringBuilder(256).append('(')
			.append('&')
			.append('(')
			.append(requireNonNull(nameAttr))
			.append('=')
			.append(requireNonNull(name))
			.append(')');
		final int len = filter.length();

		final String versionAttrName = Optional
			.ofNullable(ResourceUtils.getVersionAttributeForNamespace(getNamespace()))
			.orElse(Constants.VERSION_ATTRIBUTE);
		appendFilterVersionRange(filter, versionAttrName, versionRange);
		final boolean versionAttrFilterAdded = filter.length() > len;

		// Attribute matching (Core 3.7.7) for wiring namespaces
		if (getNamespace().startsWith("osgi.wiring.")) {
			MapStream<String, String> attrStream = MapStream.ofNullable(attrs)
				.filterKey(Attrs::isAttribute);
			if (versionAttrFilterAdded) {
				attrStream = attrStream.filterKey(attrName -> !attrName.equals(versionAttrName));
			}
			attrStream.sortedByKey()
				.forEachOrdered((attrName, attrValue) -> {
					switch (attrName) {
						case PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE :
						case AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE :
							appendFilterVersionRange(filter, attrName, attrValue);
							break;
						default :
							appendFilterEquals(filter, attrName, attrValue);
							break;
					}
				});
		}

		String value = (filter.length() > len) ? filter.append(')')
			.toString() : filter.substring(2);
		addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, value);
	}

	private static void appendFilterEquals(StringBuilder filter, String name, String value) {
		if (value != null) {
			filter.append('(')
				.append(name)
				.append('=');
			escapeFilterValue(filter, value).append(')');
		}
	}

	private static void appendFilterVersionRange(StringBuilder filter, String name, String versionRange) {
		Optional.ofNullable(versionRange)
			.filter(VersionRange::isOSGiVersionRange)
			.map(VersionRange::parseOSGiVersionRange)
			.map(range -> range.toFilter(name))
			.ifPresent(range -> {
				if ((filter.length() > 1) && (filter.charAt(1) == '&') && (range.length() > 1)
					&& (range.charAt(1) == '&')) {
					filter.append(range, 2, range.length() - 1);
				} else {
					filter.append(range);
				}
			});
	}

	/**
	 * If value must contain one of the characters reverse solidus ('\' \u005C),
	 * asterisk ('*' \u002A), parentheses open ('(' \u0028) or parentheses close
	 * (')' \u0029), then these characters should be preceded with the reverse
	 * solidus ('\' \u005C) character. Spaces are significant in value. Space
	 * characters are defined by Character.isWhiteSpace().
	 */
	public static String escapeFilterValue(String value) {
		final int len = value.length();
		StringBuilder sb = escapeFilterValue(new StringBuilder(len), value);
		return (len == sb.length()) ? value : sb.toString();
	}

	private static StringBuilder escapeFilterValue(StringBuilder sb, String value) {
		final int len = value.length();
		for (int i = 0; i < len; i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\\' :
				case '*' :
				case '(' :
				case ')' :
					sb.append('\\')
						.append(c);
					break;
				default :
					sb.append(c);
					break;
			}
		}
		return sb;
	}

	public void and(String... s) {
		StringBuilder filter = new StringBuilder().append('(')
			.append('&');
		String previous = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (previous != null) {
			filter.append(previous);
		}
		for (String subexpr : s) {
			filter.append(subexpr);
		}
		filter.append(')');
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
		String versionAttrName = Optional.ofNullable(ResourceUtils.getVersionAttributeForNamespace(getNamespace()))
			.orElse(Constants.VERSION_ATTRIBUTE);

		Attrs attrs = new Attrs();

		attributes.forEach((key, value) -> attrs.putTyped(key,
			(key.equals(versionAttrName) || (value instanceof Version)) ? toBndVersions(value) : value));

		directives.forEach((key, value) -> attrs.put(key.concat(":"), value));

		return attrs;
	}

	private Object toBndVersions(Object value) {
		if (value instanceof aQute.bnd.version.Version)
			return value;

		if (value instanceof Version) {
			Version osgiVersion = (Version) value;
			return new aQute.bnd.version.Version(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro(),
				osgiVersion.getQualifier());
		}

		if (value instanceof String)
			return new aQute.bnd.version.Version((String) value);

		if (value instanceof Number)
			try {
				return new aQute.bnd.version.Version(((Number) value).intValue());
			} catch (Exception e) {
				return value;
			}

		if (value instanceof Collection) {
			Collection<?> v = (Collection<?>) value;
			if (v.isEmpty())
				return value;

			if (v.iterator()
				.next() instanceof aQute.bnd.version.Version)
				return value;

			List<Object> bnds = new ArrayList<>();
			for (Object m : v) {
				bnds.add(toBndVersions(m));
			}
			return bnds;
		}

		throw new IllegalArgumentException("cannot convert " + value + " to a bnd Version(s) object as requested");
	}

	private Object toVersions(Object value) {
		if (value instanceof Version)
			return value;

		if (value instanceof aQute.bnd.version.Version) {
			aQute.bnd.version.Version bndVersion = (aQute.bnd.version.Version) value;
			return new Version(bndVersion.getMajor(), bndVersion.getMinor(), bndVersion.getMicro(),
				bndVersion.getQualifier());
		}

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

			List<Object> osgis = new ArrayList<>();
			for (Object m : v) {
				osgis.add(toVersions(m));
			}
			return osgis;
		}

		throw new IllegalArgumentException(
			"cannot convert " + value + " to a org.osgi.framework Version(s) object as requested");
	}

	public static RequirementBuilder createRequirementFromCapability(Capability capability) {
		final String namespace = capability.getNamespace();
		RequirementBuilder builder = new RequirementBuilder(namespace);
		final String versionAttrName = Optional.ofNullable(ResourceUtils.getVersionAttributeForNamespace(namespace))
			.orElse(Constants.VERSION_ATTRIBUTE);
		Map<String, Object> capAttributes = capability.getAttributes();
		StringBuilder filter = new StringBuilder(256);
		if (capAttributes.size() > 1) {
			filter.append('(')
				.append('&');
		}
		capAttributes.forEach((name, v) -> {
			if (v instanceof Version || name.equals(versionAttrName)
				|| (namespace.equals(PackageNamespace.PACKAGE_NAMESPACE)
					&& name.equals(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))) {
				appendFilterVersionRange(filter, name, v.toString());
			} else {
				appendFilterEquals(filter, name, v.toString());
			}
		});
		if (capAttributes.size() > 1) {
			filter.append(')');
		}

		builder.filter(filter);
		return builder;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		sb.append('[')
			.append(namespace)
			.append(']')
			.append(attributes)
			.append(directives);
		return sb.toString();
	}
}
