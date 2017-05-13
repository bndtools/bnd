package aQute.bnd.osgi.resource;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import aQute.bnd.version.VersionRange;
import aQute.lib.converter.Converter;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;

public class CapReqBuilder {

	private final String				namespace;
	private Resource					resource;
	private final Map<String,Object>	attributes	= new HashMap<String,Object>();
	private final Map<String,String>	directives	= new HashMap<String,String>();

	public CapReqBuilder(String namespace) {
		this.namespace = namespace;
	}

	public CapReqBuilder(String ns, Attrs attrs) throws Exception {
		this.namespace = ns;
		for (Entry<String,String> entry : attrs.entrySet()) {
			String key = entry.getKey();
			if (key.endsWith(":"))
				addDirective(key.substring(0, key.length() - 1), entry.getValue());
			else
				addAttribute(key, entry.getValue());
		}
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

		if (value.getClass().isArray()) {
			value = Converter.cnv(List.class, value);
		}

		if ("version".equals(name)) {
			value = toVersions(value);
		}

		attributes.put(name, value);
		return this;
	}

	public boolean isVersion(Object value) {
		if (value instanceof Version)
			return true;

		if (value instanceof Collection) {
			if (((Collection< ? >) value).isEmpty())
				return true;

			return isVersion(((Collection< ? >) value).iterator().next());
		}
		if (value.getClass().isArray()) {
			if (Array.getLength(value) == 0)
				return true;

			return isVersion(((Object[]) value)[0]);
		}
		return false;
	}

	public CapReqBuilder addAttributes(Map< ? extends String, ? extends Object> attributes) throws Exception {
		for (Entry< ? extends String, ? extends Object> e : attributes.entrySet()) {
			addAttribute(e.getKey(), e.getValue());
		}
		return this;
	}

	public CapReqBuilder addDirective(String name, String value) {
		if (value == null)
			return this;

		directives.put(ResourceUtils.stripDirective(name), value);
		return this;
	}

	public CapReqBuilder addDirectives(Attrs directives) {
		for (Entry<String,String> e : directives.entrySet()) {
			String key = Attrs.toDirective(e.getKey());
			if (key != null)
				addDirective(key, e.getValue());
		}
		return this;
	}

	public CapReqBuilder addDirectives(Map<String,String> directives) {
		for (Entry<String,String> e : directives.entrySet()) {
			String key = e.getKey();
			if (key.endsWith(":"))
				key = key.substring(0, key.length() - 1);
			addDirective(key, e.getValue());
		}
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
			filter = new AndFilter().addChild(bsnFilter).addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = bsnFilter;

		return new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
				.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

	}

	public static CapReqBuilder createSimpleRequirement(String ns, String name, String range) {
		Filter filter;
		SimpleFilter bsnFilter = new SimpleFilter(ns, name);
		if (range != null)
			filter = new AndFilter().addChild(bsnFilter).addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = bsnFilter;

		return new CapReqBuilder(ns).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

	}

	public CharSequence and(Object... exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append("(&");
		for (Object expr : exprs) {
			sb.append("(").append(toFilter(expr)).append(")");
		}
		sb.append(")");
		return sb;
	}

	public CharSequence or(Object... exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append("(|");
		for (Object expr : exprs) {
			sb.append("(").append(toFilter(expr)).append(")");
		}
		sb.append(")");
		return sb;
	}

	public CharSequence not(Object expr) {
		StringBuilder sb = new StringBuilder();
		sb.append("(!(").append(toFilter(expr)).append(")");
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

	public static List<Requirement> getRequirementsFrom(Parameters rr) throws Exception {
		List<Requirement> requirements = new ArrayList<Requirement>();
		for (Entry<String,Attrs> e : rr.entrySet()) {
			requirements.add(getRequirementFrom(Processor.removeDuplicateMarker(e.getKey()), e.getValue()));
		}
		return requirements;
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs) throws Exception {
		CapReqBuilder builder = createCapReqBuilder(namespace, attrs);
		return builder.buildSyntheticRequirement();
	}

	public static CapReqBuilder createCapReqBuilder(String namespace, Attrs attrs) throws Exception {
		CapReqBuilder builder = new CapReqBuilder(namespace);
		for (Entry<String,String> entry : attrs.entrySet()) {
			String key = entry.getKey();
			if (key.endsWith(":")) {
				key = key.substring(0, key.length() - 1);
				builder.addDirective(key, entry.getValue());
			} else {
				builder.addAttribute(key, entry.getValue());
			}
		}
		return builder;
	}

	public static List<Capability> getCapabilitiesFrom(Parameters rr) throws Exception {
		List<Capability> capabilities = new ArrayList<>();
		for (Entry<String,Attrs> e : rr.entrySet()) {
			capabilities.add(getCapabilityFrom(Processor.removeDuplicateMarker(e.getKey()), e.getValue()));
		}
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
			return from.setResource(r).buildCapability();
	}

	public static Requirement copy(Requirement c, Resource r) throws Exception {
		CapReqBuilder from = new CapReqBuilder(c.getNamespace()).from(c);
		if (r == null)
			return from.buildSyntheticRequirement();
		else
			return from.setResource(r).buildRequirement();
	}

	/**
	 * In bnd, we only use one map for both directives & attributes. This method
	 * will properly dispatch them AND take care of typing
	 * 
	 * @param attrs
	 * @throws Exception
	 */
	public void addAttributesOrDirectives(Attrs attrs) throws Exception {
		for (Entry<String,String> e : attrs.entrySet()) {
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
		List<String> parts = new ArrayList<String>();

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
	 * 
	 */

	static Pattern ESCAPE_FILTER_VALUE_P = Pattern.compile("[\\\\()*]");

	public static String escapeFilterValue(String value) {
		return ESCAPE_FILTER_VALUE_P.matcher(value).replaceAll("\\\\$0");
	}

	public void and(String... s) {
		String previous = directives == null ? null : directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		StringBuilder filter = new StringBuilder();

		if (previous != null) {
			filter.append("(&").append(previous);
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

		if (attributes != null) {
			for (Entry<String,Object> e : attributes.entrySet()) {
				Object value = e.getValue();

				if (e.getKey().equals("version") || value instanceof Version)
					value = toBndVersions(value);

				attrs.putTyped(e.getKey(), value);
			}
		}

		if (directives != null)
			for (Entry<String,String> e : directives.entrySet()) {
				attrs.put(e.getKey() + ":", e.getValue());
			}

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
			for (Object m : (Collection< ? >) value) {
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

		if (value instanceof Collection) {
			Collection< ? > v = (Collection< ? >) value;
			if (v.isEmpty())
				return value;

			if (v.iterator().next() instanceof Version)
				return value;

			List<Version> osgis = new ArrayList<>();
			for (Object m : (Collection< ? >) value) {
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
		for (Entry<String,Object> e : cap.getAttributes().entrySet()) {
			Object v = e.getValue();
			if (v instanceof Version || e.getKey().equals("version")) {
				VersionRange r = new VersionRange(v.toString());
				String filter = r.toFilter();
				sb.append(filter);

			} else
				sb.append("(").append(e.getKey()).append("=").append(escapeFilterValue((String) v)).append(")");
		}
		sb.append(")");

		req.and(sb.toString());
		return req;
	}
}
