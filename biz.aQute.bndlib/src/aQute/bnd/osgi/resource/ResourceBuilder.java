package aQute.bnd.osgi.resource;

import static aQute.bnd.osgi.Constants.DUPLICATE_MARKER;
import static aQute.bnd.osgi.Constants.MIME_TYPE_BUNDLE;
import static aQute.bnd.osgi.Constants.MIME_TYPE_JAR;
import static aQute.lib.exceptions.BiConsumerWithException.asBiConsumer;
import static aQute.lib.exceptions.BiFunctionWithException.asBiFunction;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.VersionRange;
import aQute.lib.converter.Converter;
import aQute.lib.filter.Filter;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class ResourceBuilder {
	private final ResourceImpl					resource		= new ResourceImpl();
	private final Map<Capability, Capability>	capabilities	= new LinkedHashMap<>();
	private final Map<Requirement, Requirement>	requirements	= new LinkedHashMap<>();
	private ReporterAdapter						reporter		= new ReporterAdapter();

	private boolean								built			= false;

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
		if (builder == null)
			return this;

		if (built)
			throw new IllegalStateException("Resource already built");

		addCapability0(builder);

		return this;
	}

	private Capability addCapability0(CapReqBuilder builder) {
		Capability cap = buildCapability(builder);
		Capability previous = capabilities.putIfAbsent(cap, cap);
		if (previous != null) {
			return previous;
		}
		return cap;
	}

	protected Capability buildCapability(CapReqBuilder builder) {
		Capability cap = builder.setResource(resource)
			.buildCapability();
		return cap;
	}

	public ResourceBuilder addRequirement(Requirement requirement) throws Exception {
		if (requirement == null)
			return this;

		CapReqBuilder builder = CapReqBuilder.clone(requirement);
		return addRequirement(builder);
	}

	public ResourceBuilder addRequirement(CapReqBuilder builder) {
		if (builder == null)
			return this;

		if (built)
			throw new IllegalStateException("Resource already built");

		addRequirement0(builder);

		return this;
	}

	private Requirement addRequirement0(CapReqBuilder builder) {
		Requirement req = buildRequirement(builder);
		Requirement previous = requirements.putIfAbsent(req, req);
		if (previous != null) {
			return previous;
		}
		return req;
	}

	protected Requirement buildRequirement(CapReqBuilder builder) {
		Requirement req = builder.setResource(resource)
			.buildRequirement();
		return req;
	}

	public Resource build() {
		if (built)
			throw new IllegalStateException("Resource already built");
		built = true;

		resource.setCapabilities(capabilities.values());
		resource.setRequirements(requirements.values());
		return resource;
	}

	public List<Capability> getCapabilities() {
		return new ArrayList<>(capabilities.values());
	}

	public List<Requirement> getRequirements() {
		return new ArrayList<>(requirements.values());
	}

	/**
	 * Parse the manifest and turn them into requirements & capabilities
	 *
	 * @param manifest The manifest to parse
	 * @throws Exception
	 */
	public boolean addManifest(Domain manifest) throws Exception {
		//
		// Identity capability
		//
		Entry<String, Attrs> bsn = manifest.getBundleSymbolicName();
		if (bsn == null) {
			reporter.warning("No BSN set, not a bundle");
			return false;
		}
		String name = bsn.getKey();
		Attrs attrs = bsn.getValue();

		CapabilityBuilder identity = new CapabilityBuilder(IdentityNamespace.IDENTITY_NAMESPACE);

		String copyright = manifest.translate(Constants.BUNDLE_COPYRIGHT);
		if (copyright != null) {
			identity.addAttribute(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE, copyright);
		}

		String description = manifest.translate(Constants.BUNDLE_DESCRIPTION);
		if (description != null) {
			identity.addAttribute(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE, description);
		}

		String docurl = manifest.get(Constants.BUNDLE_DOCURL);
		if (docurl != null) {
			identity.addAttribute(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE, docurl);
		}

		String license = manifest.get(Constants.BUNDLE_LICENSE);
		if (license != null) {
			identity.addAttribute(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE, license);
		}

		// Add all attributes from Bundle-SymbolicName header
		identity.addAttributes(attrs);
		identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, name);

		String versionString = manifest.getBundleVersion();
		if ((versionString != null) && !aQute.bnd.version.Version.isVersion(versionString)) {
			throw new IllegalArgumentException("Invalid version in bundle " + bsn + ": " + versionString);
		}
		Version version = Version.parseVersion(versionString);
		identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);

		boolean singleton = "true".equals(attrs.get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE + ":"));
		if (singleton) {
			identity.addDirective(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE, "true");
		}

		Entry<String, Attrs> fragment = manifest.getFragmentHost();
		identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
			(fragment == null) ? IdentityNamespace.TYPE_BUNDLE : IdentityNamespace.TYPE_FRAGMENT);

		addCapability(identity);

		if (fragment == null) {
			//
			// Bundle and Host capabilities
			//
			CapabilityBuilder bundle = new CapabilityBuilder(BundleNamespace.BUNDLE_NAMESPACE);
			bundle.addAttributesOrDirectives(attrs);
			bundle.removeDirective(Namespace.CAPABILITY_USES_DIRECTIVE)
				.removeDirective(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
			bundle.addAttribute(BundleNamespace.BUNDLE_NAMESPACE, name);
			bundle.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version);
			if (singleton) {
				bundle.addDirective(BundleNamespace.CAPABILITY_SINGLETON_DIRECTIVE, "true");
			}
			addCapability(bundle);

			CapabilityBuilder host = new CapabilityBuilder(HostNamespace.HOST_NAMESPACE);
			host.addAttributesOrDirectives(attrs);
			host.removeDirective(Namespace.CAPABILITY_USES_DIRECTIVE)
				.removeDirective(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
			host.addAttribute(HostNamespace.HOST_NAMESPACE, name);
			host.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version);
			addCapability(host);
		} else {
			//
			// Host requirement
			//
			addFragmentHost(fragment.getKey(), fragment.getValue());
		}

		//
		// Bundle requirements
		//
		addRequireBundles(manifest.getRequireBundle());

		//
		// Import/Export service
		//

		@SuppressWarnings("deprecation")
		Parameters importServices = OSGiHeader.parseHeader(manifest.get(Constants.IMPORT_SERVICE));
		addImportServices(importServices);

		@SuppressWarnings("deprecation")
		Parameters exportServices = OSGiHeader.parseHeader(manifest.get(Constants.EXPORT_SERVICE));
		addExportServices(exportServices);

		//
		// Package capabilities
		//

		addExportPackages(manifest.getExportPackage(), name, version);

		//
		// Package requirements
		//

		addImportPackages(manifest.getImportPackage());

		//
		// Provided capabilities
		//

		addProvideCapabilities(manifest.getProvideCapability());

		//
		// Required capabilities
		//

		addRequireCapabilities(manifest.getRequireCapability());

		//
		// Manage native code header
		//

		addRequirement(getNativeCode(manifest.getBundleNative()));

		return true;
	}

	public void addExportServices(Parameters exportServices) throws Exception {
		exportServices.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered(asBiConsumer((service, attrs) -> {
				CapabilityBuilder cb = new CapabilityBuilder(ServiceNamespace.SERVICE_NAMESPACE);
				cb.addAttributesOrDirectives(attrs);
				cb.addAttribute(Constants.OBJECTCLASS, service);
				addCapability(cb);
			}));
	}

	public void addImportServices(Parameters importServices) {
		importServices.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered((service, attrs) -> {
				boolean optional = Constants.RESOLUTION_OPTIONAL.equals(attrs.get("availability:"));
				boolean multiple = "true".equalsIgnoreCase(attrs.get("multiple:"));

				StringBuilder filter = new StringBuilder();
				filter.append('(')
					.append(Constants.OBJECTCLASS)
					.append('=')
					.append(service)
					.append(')');
				RequirementBuilder rb = new RequirementBuilder(ServiceNamespace.SERVICE_NAMESPACE);
				rb.addFilter(filter.toString());
				rb.addDirective("effective", "active");
				if (optional)
					rb.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);

				rb.addDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
					multiple ? Namespace.CARDINALITY_MULTIPLE : Namespace.CARDINALITY_SINGLE);

				addRequirement(rb);
			});
	}

	/**
	 * Caclulate the requirement from a native code header
	 *
	 * @param header the Bundle-NativeCode header or null
	 * @return a Requirement Builder set to the requirements according tot he
	 *         core spec
	 */
	public RequirementBuilder getNativeCode(String header) throws Exception {
		if (header == null || header.isEmpty())
			return null;

		Parameters bundleNative = OSGiHeader.parseHeader(header, null, new Parameters(true));
		if (bundleNative.isEmpty())
			return null;

		boolean optional = false;
		List<String> options = new LinkedList<>();

		RequirementBuilder rb = new RequirementBuilder(NativeNamespace.NATIVE_NAMESPACE);
		FilterBuilder sb = new FilterBuilder();
		sb.or();

		for (Entry<String, Attrs> entry : bundleNative.entrySet()) {

			String name = Processor.removeDuplicateMarker(entry.getKey());
			if ("*".equals(name)) {
				optional = true;
				continue;
			}

			sb.and();
			/*
			 * • osname - Name of the operating system. The value of this
			 * attribute must be the name of the operating system upon which the
			 * native libraries run. A number of canonical names are defined in
			 * Table 4.3.
			 */
			doOr(sb, "osname", NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, entry.getValue());
			/*
			 * • processor - The processor architecture. The value of this
			 * attribute must be the name of the processor architecture upon
			 * which the native libraries run. A number of canonical names are
			 * defined in Table 4.2.
			 */
			doOr(sb, "processor", NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, entry.getValue());
			/*
			 * • language - The ISO code for a language. The value of this
			 * attribute must be the name of the language for which the native
			 * libraries have been localized.
			 */
			doOr(sb, "language", NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE, entry.getValue());

			for (String key : entry.getValue()
				.keySet()) {
				Object value = entry.getValue()
					.getTyped(key);
				key = Processor.removeDuplicateMarker(key);

				switch (key) {
					case "osname" :
					case "processor" :
					case "language" :
						break;

					/*
					 * • osversion - The operating system version. The value of
					 * this attribute must be a version range as defined in
					 * Version Ranges on page 36.
					 */
					case "osversion" :
						sb.eq(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE, value);
						break;

					/*
					 * • selection-filter - A selection filter. The value of
					 * this attribute must be a filter expression that in-
					 * dicates if the native code clause should be selected or
					 * not.
					 */
					case "selection-filter" :
						String filter = value.toString();
						String validateFilter = Verifier.validateFilter(filter);
						if (validateFilter != null) {
							reporter.error("Invalid 'selection-filter' on Bundle-NativeCode %s", filter);
						}
						sb.literal(filter);
						break;

					default :
						reporter.warning("Unknown attribute on Bundle-NativeCode header %s=%s", key, value);
						break;
				}
			}
			sb.endAnd();
		}
		sb.endOr();
		if (optional)
			rb.addDirective("resolution", "optional");

		rb.addFilter(sb.toString());
		return rb;
	}

	private static void doOr(FilterBuilder sb, String key, String attribute, Attrs attrs) throws Exception {
		sb.or();

		while (attrs.containsKey(key)) {
			String[] names = Converter.cnv(String[].class, attrs.getTyped(key));
			for (String name : names) {
				sb.approximate(attribute, name);
			}
			key += DUPLICATE_MARKER;
		}

		sb.endOr();
	}

	/**
	 * Add the Require-Bundle header
	 *
	 * @throws Exception
	 */

	public void addRequireBundles(Parameters requireBundle) throws Exception {
		requireBundle.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered(asBiConsumer(this::addRequireBundle));
	}

	public void addRequireBundle(String bsn, VersionRange range) throws Exception {
		Attrs attrs = new Attrs();
		attrs.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, range.toString());
		addRequireBundle(bsn, attrs);
	}

	public void addRequireBundle(String bsn, Attrs attrs) throws Exception {
		RequirementBuilder require = new RequirementBuilder(BundleNamespace.BUNDLE_NAMESPACE);
		require.addDirectives(attrs)
			.removeDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)
			.removeDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		require.addFilter(BundleNamespace.BUNDLE_NAMESPACE, bsn,
			attrs.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE), attrs);
		addRequirement(require);
	}

	public void addFragmentHost(String bsn, Attrs attrs) throws Exception {
		RequirementBuilder require = new RequirementBuilder(HostNamespace.HOST_NAMESPACE);
		require.addDirectives(attrs)
			.removeDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)
			.removeDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		require.addDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
		require.addFilter(HostNamespace.HOST_NAMESPACE, bsn,
			attrs.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE), attrs);
		addRequirement(require);
	}

	public void addRequireCapabilities(Parameters required) throws Exception {
		required.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered(asBiConsumer((namespace, attrs) -> addRequireCapability(namespace, namespace, attrs)));
	}

	public void addRequireCapability(String namespace, String name, Attrs attrs) throws Exception {
		RequirementBuilder req = new RequirementBuilder(namespace);
		req.addAttributesOrDirectives(attrs);
		addRequirement(req);
	}

	public List<Capability> addProvideCapabilities(Parameters capabilities) throws Exception {
		List<Capability> added = capabilities.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.mapToObj(asBiFunction(this::addProvideCapability))
			.collect(toList());
		return added;
	}

	public List<Capability> addProvideCapabilities(String clauses) throws Exception {
		return addProvideCapabilities(new Parameters(clauses, reporter));
	}

	public Capability addProvideCapability(String namespace, Attrs attrs) throws Exception {
		CapabilityBuilder builder = new CapabilityBuilder(namespace);
		builder.addAttributesOrDirectives(attrs);
		addCapability(builder);
		return buildCapability(builder);
	}

	/**
	 * Add Exported Packages
	 *
	 * @throws Exception
	 */
	public void addExportPackages(Parameters exports) throws Exception {
		exports.forEach(asBiConsumer((name, attrs) -> addExportPackage(Processor.removeDuplicateMarker(name), attrs)));
	}

	public void addExportPackages(Parameters exports, String bundle_symbolic_name, Version bundle_version)
		throws Exception {
		exports.forEach(asBiConsumer((name, attrs) -> addExportPackage(Processor.removeDuplicateMarker(name), attrs,
			bundle_symbolic_name, bundle_version)));
	}

	public void addEE(EE ee) throws Exception {
		addExportPackages(ee.getPackages());
		EE[] compatibles = ee.getCompatible();
		addExecutionEnvironment(ee);
		for (EE compatible : compatibles) {
			addExecutionEnvironment(compatible);
		}
	}

	public void addExportPackage(String name, Attrs attrs, String bundle_symbolic_name, Version bundle_version)
		throws Exception {
		CapabilityBuilder builder = CapReqBuilder.createPackageCapability(name, attrs, bundle_symbolic_name,
			bundle_version);
		addCapability(builder);
	}

	public void addExportPackage(String name, Attrs attrs) throws Exception {
		addExportPackage(name, attrs, null, null);
	}

	/**
	 * Add imported packages
	 *
	 * @throws Exception
	 */
	public void addImportPackages(Parameters imports) throws Exception {
		imports.forEach(asBiConsumer((name, attrs) -> addImportPackage(Processor.removeDuplicateMarker(name), attrs)));
	}

	public Requirement addImportPackage(String name, Attrs attrs) throws Exception {
		RequirementBuilder builder = CapReqBuilder.createPackageRequirement(name, attrs, null);
		addRequirement(builder);
		return buildRequirement(builder);
	}

	// Correct version according to R5 specification section 3.4.1
	// BREE J2SE-1.4 ==> osgi.ee=JavaSE, version:Version=1.4
	// See bug 329, https://github.com/bndtools/bnd/issues/329
	public void addExecutionEnvironment(EE ee) throws Exception {
		CapReqBuilder builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getCapabilityName());
		builder.addAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, ee.getCapabilityVersion());
		addCapability(builder);

		// Compatibility with old version...
		builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
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

		List<Capability> capabilities = new ArrayList<>();
		Filter f = new Filter(filter);

		for (Capability c : getCapabilities()) {
			if (ns != null && !ns.equals(c.getNamespace()))
				continue;

			Map<String, Object> attributes = c.getAttributes();
			if (attributes != null) {
				if (f.matchMap(attributes))
					capabilities.add(c);
			}
		}
		return capabilities;
	}

	public Map<Capability, Capability> from(Resource bundle) throws Exception {
		Map<Capability, Capability> mapping = new HashMap<>();

		addRequirements(bundle.getRequirements(null));

		for (Capability c : bundle.getCapabilities(null)) {
			CapReqBuilder clone = CapReqBuilder.clone(c);
			Capability addedCapability = addCapability0(clone);
			mapping.put(c, addedCapability);
		}
		return mapping;
	}

	public Reporter getReporter() {
		return reporter;
	}

	public void addContentCapability(URI uri, String sha256, long length, String mime) throws Exception {

		assert uri != null;
		assert sha256 != null && sha256.length() == 64;
		assert length >= 0;

		CapabilityBuilder c = new CapabilityBuilder(ContentNamespace.CONTENT_NAMESPACE);
		c.addAttribute(ContentNamespace.CONTENT_NAMESPACE, sha256);
		c.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri.toString());
		c.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, Long.valueOf(length));
		c.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mime != null ? mime : MIME_TYPE_BUNDLE);
		addCapability(c);
	}

	public boolean addFile(File file, URI uri) throws Exception {
		if (uri == null)
			uri = file.toURI();

		Domain manifest = Domain.domain(file);
		boolean hasIdentity = false;
		if (manifest != null) {
			hasIdentity = addManifest(manifest);
		}
		String mime = hasIdentity ? MIME_TYPE_BUNDLE : MIME_TYPE_JAR;
		String sha256 = SHA256.digest(file)
			.asHex();
		addContentCapability(uri, sha256, file.length(), mime);
		return hasIdentity;
	}

	public ResourceBuilder safeResourceBuilder() {
		return new SafeResourceBuilder();
	}

	private class SafeResourceBuilder extends ResourceBuilder {

		@Override
		public Resource build() {
			return null;
		}

		@Override
		public ResourceBuilder addCapability(Capability capability) throws Exception {
			return ResourceBuilder.this.addCapability(capability);
		}

		@Override
		public ResourceBuilder addCapability(CapReqBuilder builder) {
			return ResourceBuilder.this.addCapability(builder);
		}

		@Override
		public int hashCode() {
			return ResourceBuilder.this.hashCode();
		}

		@Override
		public ResourceBuilder addRequirement(Requirement requirement) throws Exception {
			return ResourceBuilder.this.addRequirement(requirement);
		}

		@Override
		public ResourceBuilder addRequirement(CapReqBuilder builder) {
			return ResourceBuilder.this.addRequirement(builder);
		}

		@Override
		public List<Capability> getCapabilities() {
			return Collections.unmodifiableList(ResourceBuilder.this.getCapabilities());
		}

		@Override
		public List<Requirement> getRequirements() {
			return Collections.unmodifiableList(ResourceBuilder.this.getRequirements());
		}

		@Override
		public boolean addManifest(Domain manifest) throws Exception {
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			return ResourceBuilder.this.equals(obj);
		}

		@Override
		public void addExportServices(Parameters exportServices) throws Exception {
			ResourceBuilder.this.addExportServices(exportServices);
		}

		@Override
		public void addImportServices(Parameters importServices) {
			ResourceBuilder.this.addImportServices(importServices);
		}

		@Override
		public RequirementBuilder getNativeCode(String header) throws Exception {
			return ResourceBuilder.this.getNativeCode(header);
		}

		@Override
		public String toString() {
			return ResourceBuilder.this.toString();
		}

		@Override
		public void addRequireBundles(Parameters requireBundle) throws Exception {
			ResourceBuilder.this.addRequireBundles(requireBundle);
		}

		@Override
		public void addRequireBundle(String bsn, VersionRange range) throws Exception {
			ResourceBuilder.this.addRequireBundle(bsn, range);
		}

		@Override
		public void addRequireBundle(String bsn, Attrs attrs) throws Exception {
			ResourceBuilder.this.addRequireBundle(bsn, attrs);
		}

		@Override
		public void addFragmentHost(String bsn, Attrs attrs) throws Exception {
			ResourceBuilder.this.addFragmentHost(bsn, attrs);
		}

		@Override
		public void addRequireCapabilities(Parameters required) throws Exception {
			ResourceBuilder.this.addRequireCapabilities(required);
		}

		@Override
		public void addRequireCapability(String namespace, String name, Attrs attrs) throws Exception {
			ResourceBuilder.this.addRequireCapability(namespace, name, attrs);
		}

		@Override
		public List<Capability> addProvideCapabilities(Parameters capabilities) throws Exception {
			return ResourceBuilder.this.addProvideCapabilities(capabilities);
		}

		@Override
		public List<Capability> addProvideCapabilities(String clauses) throws Exception {
			return ResourceBuilder.this.addProvideCapabilities(clauses);
		}

		@Override
		public Capability addProvideCapability(String namespace, Attrs attrs) throws Exception {
			return ResourceBuilder.this.addProvideCapability(namespace, attrs);
		}

		@Override
		public void addExportPackages(Parameters exports, String bundle_symbolic_name, Version bundle_version)
			throws Exception {
			ResourceBuilder.this.addExportPackages(exports, bundle_symbolic_name, bundle_version);
		}

		@Override
		public void addExportPackages(Parameters exports) throws Exception {
			ResourceBuilder.this.addExportPackages(exports);
		}

		@Override
		public void addEE(EE ee) throws Exception {
			ResourceBuilder.this.addEE(ee);
		}

		@Override
		public void addExportPackage(String name, Attrs attrs, String bundle_symbolic_name,
			Version bundle_version) throws Exception {
			ResourceBuilder.this.addExportPackage(name, attrs, bundle_symbolic_name, bundle_version);
		}

		@Override
		public void addExportPackage(String name, Attrs attrs) throws Exception {
			ResourceBuilder.this.addExportPackage(name, attrs);
		}

		@Override
		public void addImportPackages(Parameters imports) throws Exception {
			ResourceBuilder.this.addImportPackages(imports);
		}

		@Override
		public Requirement addImportPackage(String name, Attrs attrs) throws Exception {
			return ResourceBuilder.this.addImportPackage(name, attrs);
		}

		@Override
		public void addExecutionEnvironment(EE ee) throws Exception {
			ResourceBuilder.this.addExecutionEnvironment(ee);
		}

		@Override
		public void addAllExecutionEnvironments(EE ee) throws Exception {
			ResourceBuilder.this.addAllExecutionEnvironments(ee);
		}

		@Override
		public void copyCapabilities(Set<String> ignoreNamespaces, Resource r) throws Exception {
			ResourceBuilder.this.copyCapabilities(ignoreNamespaces, r);
		}

		@Override
		public void addCapabilities(List<Capability> capabilities) throws Exception {
			ResourceBuilder.this.addCapabilities(capabilities);
		}

		@Override
		public void addRequirement(List<Requirement> requirements) throws Exception {
			ResourceBuilder.this.addRequirement(requirements);
		}

		@Override
		public void addRequirements(List<Requirement> requires) throws Exception {
			ResourceBuilder.this.addRequirements(requires);
		}

		@Override
		public List<Capability> findCapabilities(String ns, String filter) throws Exception {
			return ResourceBuilder.this.findCapabilities(ns, filter);
		}

		@Override
		public Map<Capability, Capability> from(Resource bundle) throws Exception {
			return ResourceBuilder.this.from(bundle);
		}

		@Override
		public Reporter getReporter() {
			return ResourceBuilder.this.getReporter();
		}

		@Override
		public void addContentCapability(URI uri, String sha256, long length, String mime) throws Exception {
			ResourceBuilder.this.addContentCapability(uri, sha256, length, mime);
		}

		@Override
		public boolean addFile(File file, URI uri) throws Exception {
			return false;
		}

	}

	/**
	 * A repository that implements the {@code WorkspaceRepositoryMarker} in the
	 * resolver must add a WORKSPACE_NAMESPACE capability to make its clear the
	 * resources are from the workspace. Ideally this would not be necessary but
	 * we're having two workspace repositories. One for Bndtools where the
	 * repository is interactive, the other is for resolving in Gradle, etc.
	 *
	 * @param name the project name
	 */
	public void addWorkspaceNamespace(String name) throws Exception {
		// Add a capability specific to the workspace so that we can
		// identify this fact later during resource processing.
		CapabilityBuilder cap = new CapabilityBuilder(ResourceUtils.WORKSPACE_NAMESPACE);
		cap.addAttribute(ResourceUtils.WORKSPACE_NAMESPACE, name);
		addCapability(cap);
	}

	@Override
	public String toString() {
		return new StringBuilder("ResourceBuilder [caps=").append(capabilities.values())
			.append(", reqs=")
			.append(requirements.values())
			.append(']')
			.toString();
	}

}
