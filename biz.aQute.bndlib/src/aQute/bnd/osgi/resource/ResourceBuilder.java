package aQute.bnd.osgi.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.VersionRange;
import aQute.lib.filter.Filter;

public class ResourceBuilder {

	private final ResourceImpl		resource		= new ResourceImpl();
	private final List<Capability>	capabilities	= new LinkedList<Capability>();
	private final List<Requirement>	requirements	= new LinkedList<Requirement>();

	private boolean					built			= false;

	public ResourceBuilder(Resource source) throws Exception {
		addCapabilities(source.getCapabilities(null));
		addRequirements(source.getRequirements(null));
	}

	public ResourceBuilder() {}

	public ResourceBuilder addCapability(Capability capability) throws Exception {
		CapReqBuilder builder = CapReqBuilder.clone(capability);
		return addCapability(builder);
	}

	public ResourceBuilder addCapability(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");

		addCapability0(builder);

		return this;
	}

	public Capability addCapability0(CapReqBuilder builder) {
		Capability cap = builder.setResource(resource).buildCapability();
		capabilities.add(cap);
		return cap;
	}

	public ResourceBuilder addRequirement(Requirement requirement) throws Exception {
		CapReqBuilder builder = CapReqBuilder.clone(requirement);
		return addRequirement(builder);
	}

	public ResourceBuilder addRequirement(CapReqBuilder builder) {
		if (built)
			throw new IllegalStateException("Resource already built");

		Requirement req = builder.setResource(resource).buildRequirement();
		requirements.add(req);

		return this;
	}

	public Resource build() {
		if (built)
			throw new IllegalStateException("Resource already built");
		built = true;

		resource.setCapabilities(capabilities);
		resource.setRequirements(requirements);
		return resource;
	}

	public List<Capability> getCapabilities() {
		return capabilities;
	}

	/**
	 * Parse the manifest and turn them into requirements & capabilities
	 * 
	 * @param manifest The manifest to parse
	 * @throws Exception
	 */
	public void addManifest(Domain manifest) throws Exception {

		//
		// Do the Bundle Identity Ns
		//

		Entry<String,Attrs> bsn = manifest.getBundleSymbolicName();
		CapReqBuilder identity = new CapReqBuilder(resource, IdentityNamespace.IDENTITY_NAMESPACE);

		if (bsn != null) {
			boolean singleton = "true".equals(bsn.getValue().get(Constants.SINGLETON_DIRECTIVE + ":"));
			boolean fragment = manifest.getFragmentHost() != null;

			//
			// First the identity
			//

			identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn.getKey());
			identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
					fragment ? IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE);

			if ("true".equals(singleton)) {
				identity.addDirective(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE, "true");
			}

			//
			// Now the provide bundle ns
			//

			CapReqBuilder provideBundle = new CapReqBuilder(resource, BundleNamespace.BUNDLE_NAMESPACE);
			provideBundle.addAttributesOrDirectives(bsn.getValue());
			addCapability(provideBundle.buildCapability());

			String version = manifest.getBundleVersion();
			if (version != null)
				identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(version));

			String copyright = manifest.get(Constants.BUNDLE_COPYRIGHT);
			if (copyright != null) {
				identity.addAttribute(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE, copyright);
			}

			String description = manifest.get(Constants.BUNDLE_DESCRIPTION);
			if (description != null) {
				identity.addAttribute(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE, description);
			}

			String docurl = manifest.get(Constants.BUNDLE_DOCURL);
			if (docurl != null) {
				identity.addAttribute(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE, docurl);
			}

			String license = manifest.get("Bundle-License");
			if (license != null) {
				identity.addAttribute(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE, license);
			}

			addCapability(identity.buildCapability());
		}

		//
		// Handle Require Bundle
		//

		Parameters requireBundle = manifest.getRequireBundle();
		addRequireBundles(requireBundle);

		//
		// Handle Fragment Host
		//

		Entry<String,Attrs> fragmentHost = manifest.getFragmentHost();
		if (fragmentHost != null)
			addFragmentHost(fragmentHost.getKey(), fragmentHost.getValue());

		//
		// Add the exported package. These need
		// to be converted to osgi.wiring.package ns
		//

		addExportPackages(manifest.getExportPackage());

		//
		// Add the imported package. These need
		// to be converted to osgi.wiring.package ns
		//

		addImportPackages(manifest.getImportPackage());

		//
		// Add the provided capabilities, they're easy!
		//

		addProvideCapabilities(manifest.getProvideCapability());

		//
		// Add the required capabilities, they're also easy!
		//

		addRequireCapabilities(manifest.getRequireCapability());
	}

	/**
	 * Add the Require-Bundle header
	 * 
	 * @throws Exception
	 */

	public void addRequireBundles(Parameters requireBundle) throws Exception {
		for (Entry<String,Attrs> clause : requireBundle.entrySet()) {
			addRequireBundle(Processor.removeDuplicateMarker(clause.getKey()), clause.getValue());
		}
	}

	public void addRequireBundle(String bsn, VersionRange range) throws Exception {
		Attrs attrs = new Attrs();
		attrs.put("bundle-version", range.toString());
		addRequireBundle(bsn, attrs);
	}

	public void addRequireBundle(String bsn, Attrs attrs) throws Exception {
		CapReqBuilder rbb = new CapReqBuilder(resource, BundleNamespace.BUNDLE_NAMESPACE);
		rbb.addAttributesOrDirectives(attrs);

		StringBuilder filter = new StringBuilder();
		filter.append("(").append(BundleNamespace.BUNDLE_NAMESPACE).append("=").append(bsn).append(")");

		String v = attrs.getVersion();
		if (v != null && VersionRange.isOSGiVersionRange(v)) {
			VersionRange range = VersionRange.parseOSGiVersionRange(v);
			filter.insert(0, "(&");
			filter.append(range.toFilter());
			filter.append(")");
		}
		rbb.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

		addRequirement(rbb.buildRequirement());
	}

	public void addFragmentHost(String bsn, Attrs attrs) throws Exception {
		CapReqBuilder rbb = new CapReqBuilder(resource, HostNamespace.HOST_NAMESPACE);
		rbb.addAttributesOrDirectives(attrs);

		StringBuilder filter = new StringBuilder();
		filter.append("(").append(HostNamespace.HOST_NAMESPACE).append("=").append(bsn).append(")");

		String v = attrs.getVersion();
		if (v != null && VersionRange.isOSGiVersionRange(v)) {
			VersionRange range = VersionRange.parseOSGiVersionRange(v);
			filter.insert(0, "(&");
			filter.append(range.toFilter());
			filter.append(")");
		}
		rbb.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

		addRequirement(rbb.buildRequirement());
	}

	public void addRequireCapabilities(Parameters required) throws Exception {
		for (Entry<String,Attrs> clause : required.entrySet()) {
			String namespace = Processor.removeDuplicateMarker(clause.getKey());
			addRequireCapability(namespace, Processor.removeDuplicateMarker(clause.getKey()), clause.getValue());
		}
	}

	public void addRequireCapability(String namespace, String name, Attrs attrs) throws Exception {
		CapReqBuilder req = new CapReqBuilder(resource, namespace);
		req.addAttributesOrDirectives(attrs);
		addRequirement(req.buildRequirement());
	}

	public List<Capability> addProvideCapabilities(Parameters capabilities) throws Exception {
		List<Capability> added = new ArrayList<>();
		for (Entry<String,Attrs> clause : capabilities.entrySet()) {
			String namespace = Processor.removeDuplicateMarker(clause.getKey());
			Attrs attrs = clause.getValue();

			Capability addedCapability = addProvideCapability(namespace, attrs);
			added.add(addedCapability);
		}
		return added;
	}

	public List<Capability> addProvideCapabilities(String clauses) throws Exception {
		return addProvideCapabilities(new Parameters(clauses));
	}

	public Capability addProvideCapability(String namespace, Attrs attrs) throws Exception {
		CapReqBuilder capb = new CapReqBuilder(resource, namespace);
		capb.addAttributesOrDirectives(attrs);
		return addCapability0(capb);
	}

	/**
	 * Add Exported Packages
	 * 
	 * @throws Exception
	 */
	public void addExportPackages(Parameters exports) throws Exception {
		for (Entry<String,Attrs> clause : exports.entrySet()) {
			String pname = Processor.removeDuplicateMarker(clause.getKey());
			Attrs attrs = clause.getValue();

			addExportPackage(pname, attrs);
		}
	}

	public void addEE(EE ee) throws Exception {
		addExportPackages(ee.getPackages());
		EE[] compatibles = ee.getCompatible();
		addExecutionEnvironment(ee);
		for (EE compatible : compatibles) {
			addExecutionEnvironment(compatible);
		}
	}

	public void addExportPackage(String packageName, Attrs attrs) throws Exception {
		CapReqBuilder capb = new CapReqBuilder(resource, PackageNamespace.PACKAGE_NAMESPACE);
		capb.addAttributesOrDirectives(attrs);
		if (!attrs.containsKey(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE)) {
			capb.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion);
		}
		capb.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, packageName);
		addCapability(capb);
	}

	/**
	 * Add imported packages
	 * 
	 * @throws Exception
	 */
	public void addImportPackages(Parameters imports) throws Exception {
		for (Entry<String,Attrs> clause : imports.entrySet()) {
			String pname = Processor.removeDuplicateMarker(clause.getKey());
			Attrs attrs = clause.getValue();

			addImportPackage(pname, attrs);
		}
	}

	public Requirement addImportPackage(String pname, Attrs attrs) throws Exception {
		CapReqBuilder reqb = new CapReqBuilder(resource, PackageNamespace.PACKAGE_NAMESPACE);
		reqb.addDirectives(attrs);
		reqb.addFilter(PackageNamespace.PACKAGE_NAMESPACE, pname, attrs.getVersion(), attrs);
		Requirement requirement = reqb.buildRequirement();
		addRequirement(requirement);
		return requirement;
	}

	// Correct version according to R5 specification section 3.4.1
	// BREE J2SE-1.4 ==> osgi.ee=JavaSE, version:Version=1.4
	// See bug 329, https://github.com/bndtools/bnd/issues/329
	public void addExecutionEnvironment(EE ee) throws Exception {

		CapReqBuilder builder = new CapReqBuilder(resource,
				ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getCapabilityName());
		builder.addAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, ee.getCapabilityVersion());
		addCapability(builder);

		// Compatibility with old version...
		builder = new CapReqBuilder(resource, ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getEEName());
		addCapability(builder);
	}

	public void addAllExecutionEnvironments(EE ee) throws Exception {
		addExportPackages(ee.getPackages());
		addExecutionEnvironment(ee);
		for (EE compatibleEE : ee.getCompatible()) {
			addExecutionEnvironment(compatibleEE);
		}
	}

	public void copyCapabilities(Set<String> ignoreNamespaces, Resource r) throws Exception {
		for (Capability c : r.getCapabilities(null)) {
			if (ignoreNamespaces.contains(c.getNamespace()))
				continue;

			addCapability(c);
		}
	}

	public void addCapabilities(List<Capability> capabilities) throws Exception {
		if (capabilities == null || capabilities.isEmpty())
			return;

		for (Capability c : capabilities)
			addCapability(c);

	}

	public void addRequirement(List<Requirement> requirements) throws Exception {
		if (requirements == null || requirements.isEmpty())
			return;

		for (Requirement rq : requirements)
			addRequirement(rq);

	}

	public void addRequirements(List<Requirement> requires) throws Exception {
		for (Requirement req : requires) {
			addRequirement(req);
		}
	}

	public List<Capability> findCapabilities(String ns, String filter) throws Exception {
		if (filter == null || capabilities.isEmpty())
			return Collections.emptyList();

		List<Capability> capabilities = new ArrayList<Capability>();
		Filter f = new Filter(filter);

		for (Capability c : getCapabilities()) {
			if (ns != null && !ns.equals(c.getNamespace()))
				continue;

			Map<String,Object> attributes = c.getAttributes();
			if (attributes != null) {
				if (f.matchMap(attributes))
					capabilities.add(c);
			}
		}
		return capabilities;
	}

	public Map<Capability,Capability> from(Resource bundle) throws Exception {
		Map<Capability,Capability> mapping = new HashMap<Capability,Capability>();

		addRequirements(bundle.getRequirements(null));

		for (Capability c : bundle.getCapabilities(null)) {
			CapReqBuilder clone = CapReqBuilder.clone(c);
			Capability addedCapability = addCapability0(clone);
			mapping.put(c, addedCapability);
		}
		return mapping;
	}

}
