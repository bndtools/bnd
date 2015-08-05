package aQute.bnd.osgi.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.VersionRange;
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

	public CapReqBuilder(String ns, Attrs attrs) {
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

	public static CapReqBuilder clone(Capability capability) {
		CapReqBuilder builder = new CapReqBuilder(capability.getNamespace());
		builder.addAttributes(capability.getAttributes());
		builder.addDirectives(capability.getDirectives());
		return builder;
	}

	public static CapReqBuilder clone(Requirement requirement) {
		CapReqBuilder builder = new CapReqBuilder(requirement.getNamespace());
		builder.addAttributes(requirement.getAttributes());
		builder.addDirectives(requirement.getDirectives());
		return builder;
	}

	public String getNamespace() {
		return namespace;
	}

	public CapReqBuilder setResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public CapReqBuilder addAttribute(String name, Object value) {
		if (value != null)
			attributes.put(name, value);
		return this;
	}

	public CapReqBuilder addAttributes(Map< ? extends String, ? extends Object> attributes) {
		this.attributes.putAll(attributes);
		return this;
	}

	public CapReqBuilder addDirective(String name, String value) {
		if (value != null)
			directives.put(name, value);
		return this;
	}

	public CapReqBuilder addDirectives(Map< ? extends String, ? extends String> directives) {
		this.directives.putAll(directives);
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
			filter = new AndFilter().addChild(pkgNameFilter).addChild(
					new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = pkgNameFilter;

		return new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE).addDirective(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
	}

	public static CapReqBuilder createBundleRequirement(String bsn, String range) {
		Filter filter;
		SimpleFilter bsnFilter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
		if (range != null)
			filter = new AndFilter().addChild(bsnFilter).addChild(new LiteralFilter(Filters.fromVersionRange(range)));
		else
			filter = bsnFilter;

		return new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

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

	public static List<Requirement> getRequirementsFrom(Parameters rr) {
		List<Requirement> requirements = new ArrayList<Requirement>();
		for (Entry<String,Attrs> e : rr.entrySet()) {
			requirements.add(getRequirementFrom(Processor.removeDuplicateMarker(e.getKey()), e.getValue()));
		}
		return requirements;
	}

	public static Requirement getRequirementFrom(String namespace, Attrs attrs) {
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
		return builder.buildSyntheticRequirement();
	}

	public CapReqBuilder from(Capability c) {
		addAttributes(c.getAttributes());
		addDirectives(c.getDirectives());
		return this;
	}

	public CapReqBuilder from(Requirement r) {
		addAttributes(r.getAttributes());
		addDirectives(r.getDirectives());
		return this;
	}

	public static Capability copy(Capability c, Resource r) {
		CapReqBuilder from = new CapReqBuilder(c.getNamespace()).from(c);
		if (r == null)
			return from.buildSyntheticCapability();
		else
			return from.setResource(r).buildCapability();
	}

	public static Requirement copy(Requirement c, Resource r) {
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
	 */
	public void addAttributesOrDirectives(Attrs attrs) {
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

	public void addFilter(String ns, String name, Attrs attrs) {
		List<String> parts = new ArrayList<String>();

		parts.add("(" + ns + "=" + name + ")");
		String v = attrs.getVersion();
		if (v != null && VersionRange.isVersionRange(v)) {
			VersionRange range = VersionRange.parseVersionRange(v);
			parts.add(range.toFilter());
		}
		for (Entry<String,String> a : attrs.entrySet()) {
			String key = a.getKey();
			parts.add("(" + key + "=" + a.getValue() + ")");
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

	public void and(String... s) {
		String previous = directives == null ? null : directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		StringBuilder filter = new StringBuilder();

		if (previous != null) {
			filter.append("(&").append(previous);
		}
		filter.append(s);
		if (previous != null) {
			filter.append(")");
		}
		addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
	}
}
