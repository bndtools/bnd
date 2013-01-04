package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.framework.namespace.*;
import org.osgi.resource.*;

import aQute.bnd.osgi.resource.CapReq.MODE;
import aQute.libg.filters.*;

public class CapReqBuilder {

	private final String				namespace;
	private Resource					resource;
	private final Map<String,Object>	attributes	= new HashMap<String,Object>();
	private final Map<String,String>	directives	= new HashMap<String,String>();

	public CapReqBuilder(String namespace) {
		this.namespace = namespace;
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
		// TODO check the thrown exception
		if (resource == null)
			throw new IllegalStateException("Cannot build Capability with null Resource.");
		return new CapReq(MODE.Capability, namespace, resource, directives, attributes);
	}

	public Requirement buildRequirement() {
		// TODO check the thrown exception
		if (resource == null)
			throw new IllegalStateException("Cannot build Requirement with null Resource.");
		return new CapReq(MODE.Requirement, namespace, resource, directives, attributes);
	}

	public Requirement buildSyntheticRequirement() {
		return new CapReq(MODE.Requirement, namespace, null, directives, attributes);
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
}
