package aQute.bnd.osgi.resource;

import static aQute.bnd.exceptions.SupplierWithException.asSupplier;
import static aQute.bnd.osgi.Constants.DUPLICATE_MARKER;
import static aQute.bnd.osgi.Constants.MIME_TYPE_BUNDLE;
import static aQute.bnd.osgi.Constants.MIME_TYPE_JAR;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.build.model.EE;
import aQute.bnd.classindex.ClassIndexerAnalyzer;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.SupplierWithException;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.JPMSModule;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.unmodifiable.Lists;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.MultiMap;
import aQute.lib.converter.Converter;
import aQute.lib.filter.Filter;
import aQute.lib.hex.Hex;
import aQute.lib.hierarchy.FolderNode;
import aQute.lib.hierarchy.Hierarchy;
import aQute.lib.hierarchy.NamedNode;
import aQute.lib.zip.JarIndex;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * The ResourceBuilder class provides a fluent API for constructing resources.
 */
@ProviderType
public class ResourceBuilder {
	public static final String						SYNTHETIC			= "bnd.synthetic";
	private static final FileResourceCache			cache				= FileResourceCache.getInstance();
	private final ResourceImpl						resource;
	private final MultiMap<String, CapabilityImpl>	capabilities		= new MultiMap<>();
	private final MultiMap<String, RequirementImpl>	requirements		= new MultiMap<>();
	private final List<Resource>					supportingResources	= new ArrayList<>();
	private ReporterAdapter							reporter			= new ReporterAdapter();
	private boolean									built				= false;
	private SupportingResource						parent				= null;

	/**
	 * Constructs a new `ResourceBuilder`.
	 */
	public ResourceBuilder() {
		this.resource = new ResourceImpl(null);
	}

	/**
	 * Constructs a new `ResourceBuilder`.
	 */
	public ResourceBuilder(SupportingResource parent) {
		this.resource = new ResourceImpl(parent);
		this.parent = parent;

	}

	/**
	 * Constructs a new `ResourceBuilder` with the given source resource.
	 *
	 * @param source the source resource to add to this builder
	 */
	@Deprecated
	public ResourceBuilder(Resource source) {
		this();
		addResource(source);
	}

	/**
	 * Adds a resource to this builder. If the resource is a
	 * `SupportingResource`, it is added as a composite resource, otherwise its
	 * capabilities and requirements are added to this builder.
	 *
	 * @param source the resource to add
	 * @return this builder
	 */
	public ResourceBuilder addResource(Resource source) {
		if (source instanceof SupportingResource cr)
			return addResource(cr);
		else {
			addCapabilities(source.getCapabilities(null));
			addRequirements(source.getRequirements(null));
			return this;
		}
	}

	/**
	 * Adds a composite resource to this builder. The capabilities,
	 * requirements, and supporting resources of the composite resource are
	 * added to this builder.
	 *
	 * @param source the composite resource to add
	 * @return this builder
	 */
	public ResourceBuilder addResource(SupportingResource source) {
		addCapabilities(source.getCapabilities(null));
		addRequirements(source.getRequirements(null));
		source.getSupportingResources()
			.forEach(this::addSupportingResource);
		return this;
	}

	/**
	 * Adds a supporting resource to this builder.
	 *
	 * @param resource the supporting resource to add
	 */
	public void addSupportingResource(Resource resource) {
		supportingResources.add(resource);
	}

	/**
	 * Adds a capability to this builder.
	 *
	 * @param capability the capability to add
	 * @return this builder
	 */

	public ResourceBuilder addCapability(Capability capability) {
		CapReqBuilder builder = CapReqBuilder.clone(capability);
		return addCapability(builder);
	}

	/**
	 * Adds a capability to this builder using the given `CapReqBuilder`.
	 *
	 * @param builder the `CapReqBuilder` to use for building the capability
	 * @return this builder
	 */

	public ResourceBuilder addCapability(CapReqBuilder builder) {
		if (builder == null)
			return this;

		if (built)
			throw new IllegalStateException("Resource already built");

		addCapability0(builder);

		return this;
	}

	private Capability addCapability0(CapReqBuilder builder) {
		CapabilityImpl cap = buildCapability(builder);
		capabilities.add(cap.getNamespace(), cap);
		return cap;
	}

	/**
	 * Builds a capability using the given `CapReqBuilder`.
	 *
	 * @param builder the `CapReqBuilder` to use for building the capability
	 * @return the built capability
	 */

	protected CapabilityImpl buildCapability(CapReqBuilder builder) {
		CapabilityImpl cap = builder.setResource(resource)
			.buildCapability();
		return cap;
	}

	/**
	 * Adds a requirement to this builder.
	 *
	 * @param requirement the requirement to add
	 * @return this builder
	 */
	public ResourceBuilder addRequirement(Requirement requirement) {
		if (requirement == null)
			return this;

		CapReqBuilder builder = CapReqBuilder.clone(requirement);
		return addRequirement(builder);
	}

	/**
	 * Adds a requirement to this ResourceBuilder.
	 *
	 * @param builder the CapReqBuilder representing the requirement to add
	 * @return this ResourceBuilder instance
	 * @throws IllegalStateException if the Resource has already been built
	 */

	public ResourceBuilder addRequirement(CapReqBuilder builder) {
		if (builder == null)
			return this;

		if (built)
			throw new IllegalStateException("Resource already built");

		addRequirement0(builder);

		return this;
	}

	/*
	 * Adds a RequirementImpl object to this ResourceBuilder's requirements
	 * list.
	 * @param builder the CapReqBuilder representing the requirement to add
	 * @return the newly created RequirementImpl object
	 */
	private Requirement addRequirement0(CapReqBuilder builder) {
		RequirementImpl req = buildRequirement(builder);
		requirements.add(req.getNamespace(), req);
		return req;
	}

	/**
	 * Builds a new RequirementImpl object using the provided CapReqBuilder.
	 *
	 * @param builder the CapReqBuilder to use to build the RequirementImpl
	 * @return the newly created RequirementImpl object
	 */
	protected RequirementImpl buildRequirement(CapReqBuilder builder) {
		RequirementImpl req = builder.setResource(resource)
			.buildRequirement();
		return req;
	}

	/**
	 * Builds and returns a new SupportingResource object using the data stored
	 * in this ResourceBuilder.
	 *
	 * @return the newly created SupportingResource object
	 * @throws IllegalStateException if the Resource has already been built
	 */
	public SupportingResource build() {
		if (built)
			throw new IllegalStateException("Resource already built");
		built = true;

		return resource.build(capabilities, requirements, supportingResources, parent);
	}

	/**
	 * Returns a flattened list of all the Capability objects stored in this
	 * ResourceBuilder.
	 *
	 * @return a List of Capability objects
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public List<Capability> getCapabilities() {
		return (List) ResourceImpl.flatten(capabilities);
	}

	/**
	 * Returns a flattened list of all the Requirement objects stored in this
	 * ResourceBuilder.
	 *
	 * @return a List of Requirement objects
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public List<Requirement> getRequirements() {
		return (List) ResourceImpl.flatten(requirements);
	}

	/**
	 * Parse the manifest and turn them into requirements & capabilities
	 *
	 * @param manifest The manifest to parse
	 */
	public boolean addManifest(Domain manifest) {

		//
		// Check if this JAR has a main class header
		//

		String mainClass = manifest.get("Main-Class");
		if (mainClass != null) {
			CapabilityBuilder mc = new CapabilityBuilder(MainClassNamespace.MAINCLASS_NAMESPACE);
			MainClassNamespace.build(mc, manifest);
			addCapability(mc);
		}

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
		Version version;
		try {
			version = Version.parseVersion(versionString);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid version in bundle " + name, e);
		}
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

	/**
	 * Adds CapabilityBuilder objects to this Processor's capabilities list, one
	 * for each service listed in the exportServices Parameters object.
	 *
	 * @param exportServices the Parameters object containing the service names
	 *            and attributes to export
	 */
	public void addExportServices(Parameters exportServices) {
		exportServices.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered((service, attrs) -> {
				CapabilityBuilder cb = new CapabilityBuilder(ServiceNamespace.SERVICE_NAMESPACE);
				cb.addAttributesOrDirectives(attrs);
				cb.addAttribute(Constants.OBJECTCLASS, service);
				addCapability(cb);
			});
	}

	/**
	 * Adds RequirementBuilder objects to this Processor's requirements list,
	 * one for each service listed in the importServices Parameters object.
	 *
	 * @param importServices the Parameters object containing the service names
	 *            and attributes to import
	 */
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
	 * Calculate the requirement from a native code header
	 *
	 * @param header the Bundle-NativeCode header or null
	 * @return a Requirement Builder set to the requirements according tot he
	 *         core spec
	 */
	public RequirementBuilder getNativeCode(String header) {
		if (header == null || header.isEmpty())
			return null;

		Parameters bundleNative = OSGiHeader.parseHeader(header, null, new Parameters(true));
		if (bundleNative.isEmpty())
			return null;

		boolean optional = false;

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

	private static void doOr(FilterBuilder sb, String key, String attribute, Attrs attrs) {
		sb.or();

		while (attrs.containsKey(key)) {
			String[] names;
			try {
				names = Converter.cnv(String[].class, attrs.getTyped(key));
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
			for (String name : names) {
				sb.approximate(attribute, name);
			}
			key += DUPLICATE_MARKER;
		}

		sb.endOr();
	}

	/**
	 * Add the Require-Bundle header
	 */

	public void addRequireBundles(Parameters requireBundle) {
		requireBundle.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered(this::addRequireBundle);
	}

	/**
	 * Adds a new Require-Bundle requirement to this Processor's requirements
	 * list, specifying the bundle symbolic name and version range.
	 *
	 * @param bsn the bundle symbolic name to require
	 * @param range the version range to require for the specified bundle
	 */
	public void addRequireBundle(String bsn, VersionRange range) {
		Attrs attrs = new Attrs();
		attrs.put(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, range.toString());
		addRequireBundle(bsn, attrs);
	}

	/**
	 * Adds a new Require-Bundle requirement to this Processor's requirements
	 * list, specifying the bundle symbolic name and any additional attributes.
	 *
	 * @param bsn the bundle symbolic name to require
	 * @param attrs the additional attributes for the requirement
	 */

	public void addRequireBundle(String bsn, Attrs attrs) {
		RequirementBuilder require = new RequirementBuilder(BundleNamespace.BUNDLE_NAMESPACE);
		require.addDirectives(attrs)
			.removeDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)
			.removeDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		require.addFilter(BundleNamespace.BUNDLE_NAMESPACE, bsn,
			attrs.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE), attrs);
		addRequirement(require);
	}

	/**
	 * Adds a new Fragment-Host requirement to this Processor's requirements
	 * list, specifying the host bundle symbolic name and any additional
	 * attributes.
	 *
	 * @param bsn the host bundle symbolic name to require
	 * @param attrs the additional attributes for the requirement
	 */
	public void addFragmentHost(String bsn, Attrs attrs) {
		RequirementBuilder require = new RequirementBuilder(HostNamespace.HOST_NAMESPACE);
		require.addDirectives(attrs)
			.removeDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)
			.removeDirective(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		require.addDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
		require.addFilter(HostNamespace.HOST_NAMESPACE, bsn,
			attrs.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE), attrs);
		addRequirement(require);
	}

	/**
	 * Adds one or more Require-Capability requirements to this Processor's
	 * requirements list, using the specified Parameters object to define the
	 * required capabilities.
	 *
	 * @param required the Parameters object containing the required
	 *            capabilities and their attributes
	 */
	public void addRequireCapabilities(Parameters required) {
		required.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.forEachOrdered((namespace, attrs) -> addRequireCapability(namespace, namespace, attrs));
	}

	/**
	 * Adds a new Require-Capability requirement to this Processor's
	 * requirements list, using the specified namespace, name, and attributes.
	 *
	 * @param namespace the namespace of the required capability
	 * @param name the name of the required capability, or null if not
	 *            applicable
	 * @param attrs the additional attributes for the requirement
	 */

	public void addRequireCapability(String namespace, String name, Attrs attrs) {
		addRequireCapability(namespace, name, null, attrs);
	}

	/**
	 * Adds a new Require-Capability requirement to this Processor's
	 * requirements list, using the specified namespace, name, version range,
	 * and attributes.
	 *
	 * @param namespace the namespace of the required capability
	 * @param name the name of the required capability, or null if not
	 *            applicable
	 * @param versionRange the version range to require for the specified
	 *            capability, or null if not applicable
	 * @param attrs the additional attributes for the requirement
	 */
	public void addRequireCapability(String namespace, String name, String versionRange, Attrs attrs) {
		RequirementBuilder req = new RequirementBuilder(namespace);
		if (name != null) {
			req.addFilter(namespace, name, versionRange, attrs);
		}
		req.addAttributesOrDirectives(attrs);
		addRequirement(req);
	}

	/**
	 * Adds one or more CapabilityBuilder objects to this Processor's
	 * capabilities list, using the specified Parameters object to define the
	 * provided capabilities.
	 *
	 * @param capabilities the Parameters object containing the provided
	 *            capabilities and their attributes
	 * @return a List of Capability objects that were added to the Processor's
	 *         capabilities list
	 */
	public List<Capability> addProvideCapabilities(Parameters capabilities) {
		List<Capability> added = capabilities.stream()
			.mapKey(Processor::removeDuplicateMarker)
			.mapToObj(this::addProvideCapability)
			.collect(toList());
		return added;
	}

	public List<Capability> addProvideCapabilities(String clauses) {
		return addProvideCapabilities(new Parameters(clauses, reporter));
	}

	/**
	 * Adds a new CapabilityBuilder object to this Processor's capabilities
	 * list, representing a provided capability with the specified namespace and
	 * attributes.
	 *
	 * @param namespace the namespace of the provided capability
	 * @param attrs the attributes for the provided capability
	 * @return the Capability object that was added to the Processor's
	 *         capabilities list
	 */

	public Capability addProvideCapability(String namespace, Attrs attrs) {
		CapabilityBuilder builder = new CapabilityBuilder(namespace);
		builder.addAttributesOrDirectives(attrs);
		addCapability(builder);
		return buildCapability(builder);
	}

	/**
	 * Adds one or more Export-Package requirements to this Processor's
	 * requirements list, using the specified Parameters object to define the
	 * exported packages and their attributes.
	 *
	 * @param exports the Parameters object containing the exported packages and
	 *            their attributes
	 */
	public void addExportPackages(Parameters exports) {
		exports.forEach((name, attrs) -> addExportPackage(Processor.removeDuplicateMarker(name), attrs));
	}

	/**
	 * Adds one or more Export-Package requirements to this Processor's
	 * requirements list, using the specified Parameters object to define the
	 * exported packages and their attributes, as well as the bundle symbolic
	 * name and version for the exporting bundle.
	 *
	 * @param exports the Parameters object containing the exported packages and
	 *            their attributes
	 * @param bundle_symbolic_name the symbolic name of the exporting bundle
	 * @param bundle_version the version of the exporting bundle
	 */
	public void addExportPackages(Parameters exports, String bundle_symbolic_name, Version bundle_version) {
		exports.forEach((name, attrs) -> addExportPackage(Processor.removeDuplicateMarker(name), attrs,
			bundle_symbolic_name, bundle_version));
	}

	/**
	 * Adds the Execution Environment information to this Processor's
	 * capabilities list and exported packages list.
	 *
	 * @param ee the EE object containing the Execution Environment information
	 */
	public void addEE(EE ee) {
		addExportPackages(ee.getPackages());

		MultiMap<String, aQute.bnd.version.Version> map = new MultiMap<>();

		Stream.concat(Stream.of(ee), Stream.of(ee.getCompatible()))
			.forEach(e -> {
				map.add(e.getCapabilityName(), e.getCapabilityVersion());

				// Compatibility with old version...

				CapReqBuilder builder = new CapReqBuilder(
					ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
				builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, e.getEEName());
				addCapability(builder);
			});

		map.forEach((k, v) -> {
			CapReqBuilder builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, k);
			builder.addAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, v);
			addCapability(builder);
		});
	}

	/**
	 * Adds a new Export-Package requirement to this Processor's requirements
	 * list, representing an exported package with the specified name and
	 * attributes, and exporting by the specified bundle.
	 *
	 * @param name the name of the exported package
	 * @param attrs the attributes for the exported package
	 * @param bundle_symbolic_name the symbolic name of the exporting bundle
	 * @param bundle_version the version of the exporting bundle
	 */
	public void addExportPackage(String name, Attrs attrs, String bundle_symbolic_name, Version bundle_version) {
		CapabilityBuilder builder = CapReqBuilder.createPackageCapability(name, attrs, bundle_symbolic_name,
			bundle_version);
		addCapability(builder);
	}

	/**
	 * Adds a new Export-Package requirement to this Processor's requirements
	 * list, representing an exported package with the specified name and
	 * attributes, and exporting by the current bundle.
	 *
	 * @param name the name of the exported package
	 * @param attrs the attributes for the exported package
	 */
	public void addExportPackage(String name, Attrs attrs) {
		addExportPackage(name, attrs, null, null);
	}

	/**
	 * Add imported packages
	 */
	public void addImportPackages(Parameters imports) {
		imports.forEach((name, attrs) -> addImportPackage(Processor.removeDuplicateMarker(name), attrs));
	}

	public Requirement addImportPackage(String name, Attrs attrs) {
		RequirementBuilder builder = CapReqBuilder.createPackageRequirement(name, attrs, null);
		addRequirement(builder);
		return buildRequirement(builder);
	}

	@Deprecated
	public void addExecutionEnvironment(EE ee) {
		addEE(ee);
	}

	@Deprecated
	public void addAllExecutionEnvironments(EE ee) {
		addEE(ee);
	}

	public void copyCapabilities(Set<String> ignoreNamespaces, Resource r) {
		for (Capability c : r.getCapabilities(null)) {
			if (ignoreNamespaces.contains(c.getNamespace()))
				continue;

			addCapability(c);
		}
	}

	public void addCapabilities(List<Capability> capabilities) {
		if (capabilities == null || capabilities.isEmpty())
			return;

		for (Capability c : capabilities)
			addCapability(c);

	}

	public void addRequirement(List<Requirement> requirements) {
		if (requirements == null || requirements.isEmpty())
			return;

		for (Requirement rq : requirements)
			addRequirement(rq);

	}

	public void addRequirements(List<Requirement> requires) {
		for (Requirement req : requires) {
			addRequirement(req);
		}
	}

	public List<Capability> findCapabilities(String ns, String filter) {
		if (filter == null || capabilities.isEmpty())
			return Lists.of();

		List<Capability> capabilities = new ArrayList<>();
		Filter f = new Filter(filter);

		for (Capability c : getCapabilities()) {
			if (ns != null && !ns.equals(c.getNamespace()))
				continue;

			Map<String, Object> attributes = c.getAttributes();
			if (attributes != null) {
				try {
					if (f.matchMap(attributes))
						capabilities.add(c);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
		}
		return capabilities;
	}

	public Map<Capability, Capability> from(Resource bundle) {
		Map<Capability, Capability> mapping = new LinkedHashMap<>();

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

	public void addContentCapability(URI uri, String sha256, long length, String mime) {

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

	void addContentCapability(URI uri, DeferredValue<String> sha256, long length, String mime) {
		assert uri != null;
		assert sha256 != null;
		assert length >= 0;

		CapabilityBuilder c = new CapabilityBuilder(ContentNamespace.CONTENT_NAMESPACE);
		c.addAttribute(ContentNamespace.CONTENT_NAMESPACE, sha256);
		c.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri.toString());
		c.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, Long.valueOf(length));
		c.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mime != null ? mime : MIME_TYPE_BUNDLE);
		addCapability(c);
	}

	public boolean addFile(File file, URI uri) throws Exception {

		SupportingResource resource = cache.getResource(file, uri, () -> parse(file, uri));
		addResource(resource);
		return resource.hasIdentity();
	}

	public boolean addFile(File f) throws Exception {
		return addFile(f, f.toURI());
	}

	public static SupportingResource parse(File file, URI uri) {

		if (file == null)
			throw new IllegalArgumentException("no file object");

		if (!file.exists())
			throw new IllegalArgumentException("no such file " + file.getAbsolutePath());

		if (file.isFile() && file.length() == 0)
			throw new IllegalArgumentException("empty file " + file.getAbsolutePath());

		if (file.isFile() && !file.canRead())
			throw new IllegalArgumentException("no read access " + file.getAbsolutePath());

		if (file.isDirectory() && !file.canRead())
			throw new IllegalArgumentException("no read access on folder " + file.getAbsolutePath());

		if (uri == null)
			uri = file.toURI();

		try (Jar jar = new Jar(file)) {
			ResourceBuilder rb = new ResourceBuilder();

			boolean hasIdentity = rb.addJar(jar);
			String mime = hasIdentity ? MIME_TYPE_BUNDLE : MIME_TYPE_JAR;

			rb.addContentCapability(uri,
				new DeferredComparableValue<String>(String.class,
					SupplierWithException.asSupplier(() -> SHA256.digest(file)
						.asHex()),
					file.hashCode()),
				file.length(), mime);

			return rb.build();
		} catch (ZipException rt) {
			// can happen if the file is not a JAR file (e.g. a dynamic libray,
			// .so, .dylib)
			ResourceBuilder rb = new ResourceBuilder();
			// placeholder for "any file"
			String mime = "application/octet-stream";
			rb.addContentCapability(uri,
				new DeferredComparableValue<String>(String.class,
					SupplierWithException.asSupplier(() -> SHA256.digest(file)
						.asHex()),
					file.hashCode()),
				file.length(), mime);
			return rb.build();
		}
		catch (Exception rt) {
			throw new IllegalArgumentException("illegal format " + file.getAbsolutePath(), rt);
		}
	}

	public boolean addJar(Jar jar) {
		try {
			Domain manifest = Domain.domain(jar.getManifest());
			if (addManifest(manifest)) {

				if (manifest.getMultiRelease()) {
					addMultiRelease(jar, manifest);
				}
				addHashes(jar);
				return true;
			}
			return false;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private void addMultiRelease(Jar jar, Domain manifest) {
		JPMSModule jpms = new JPMSModule(jar);

		String bsn = manifest.getBundleSymbolicName()
			.getKey();
		Version version = Version.parseVersion(manifest.getBundleVersion());
		VersionRange capabilityRange = new VersionRange(true, version, version, true);

		SortedSet<EE> ees = Collections.emptySortedSet();

		List<RequirementImpl> list = requirements.remove(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		if (list != null && list.size() > 0) {
			RequirementImpl remove = list.remove(0);
			ees = EE.getEEsFromRequirement(remove.toString());
		}
		if (ees.isEmpty()) {
			ees = EE.all();
		}

		RequirementBuilder rqb = new RequirementBuilder(MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE);
		FilterBuilder fb = new FilterBuilder();
		fb.and()
			.eq(MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE, bsn)
			.in(MultiReleaseNamespace.CAPABILITY_VERSION_ATTRIBUTE, capabilityRange);
		rqb.addFilter(fb);
		addRequirement(rqb);

		int base = ees.first()
			.getRelease();

		addSupportingResource(buildSupportingResource(jpms, bsn, version, base, manifest, resource));

		for (int release : jpms.getVersions()) {
			SupportingResource build = buildSupportingResource(jpms, bsn, version, release, manifest, resource);
			addSupportingResource(build);
		}
	}

	static SupportingResource buildSupportingResource(JPMSModule jpms, String bsn, Version version, int release,
		Domain manifest, ResourceImpl parent) {
		ResourceBuilder builder = new ResourceBuilder(parent);
		Domain m = Domain.domain(jpms.getManifest(release));

		CapabilityBuilder cb = new CapabilityBuilder(MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE);
		cb.addAttribute(MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE, bsn);
		cb.addAttribute(MultiReleaseNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		builder.addCapability(cb);

		CapabilityBuilder id = new CapabilityBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		id.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn + "__" + release);
		id.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		id.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, SYNTHETIC);
		builder.addCapability(id);

		builder.addImportPackages(m.getImportPackage()
			.removeAll(manifest.getImportPackage()));
		builder.addRequireCapabilities(m.getRequireCapability()
			.removeAll(manifest.getRequireCapability()));
		builder.requirements.remove(EXECUTION_ENVIRONMENT_NAMESPACE);

		RequirementBuilder rqb = new RequirementBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		EE ee = EE.getEEFromReleaseVersion(release);
		aQute.bnd.version.Version low = ee.getCapabilityVersion();
		VersionRange eeRange;
		int nextRelease = jpms.getNextRelease(release);
		if (nextRelease == Integer.MAX_VALUE)
			eeRange = new VersionRange(low);
		else
			eeRange = new VersionRange(low, new aQute.bnd.version.Version(nextRelease, 0, 0));

		FilterBuilder fb = new FilterBuilder();
		fb.and()
			.eq(EXECUTION_ENVIRONMENT_NAMESPACE, EE.JavaSE_9.getCapabilityName())
			.in(CAPABILITY_VERSION_ATTRIBUTE, eeRange)
			.endAnd();
		rqb.addFilter(fb);
		builder.addRequirement(rqb);

		builder.addSyntheticContentCapability("urn:osgi-bnd-mrj:" + bsn + ":" + version + ":" + release);
		return builder.build();
	}

	/*
	 * Pass a urn that is unique for this resource.
	 */
	private void addSyntheticContentCapability(String urn) {
		try {
			URI uri = URI.create(urn);
			String sha = SHA256.digest(urn.getBytes(StandardCharsets.UTF_8))
				.asHex();
			addContentCapability(uri, sha, 0, "application/octetstream");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public boolean addManifest(Manifest m) {
		return addManifest(Domain.domain(m));

	}

	public void addHashes(File file) {
		addHashes(asSupplier(() -> new JarIndex(file)));
	}

	public void addHashes(Jar jar) {
		addHashes(asSupplier(() -> new JarIndex(jar.getResources())));
	}

	/**
	 * Add simple class name hashes to the exported packages. This should not be
	 * called before any package capabilities are set since we only hash class
	 * names in exports. So no exports, no hash.
	 */
	private void addHashes(Supplier<JarIndex> supplier) {

		List<CapabilityImpl> packageCapabilities = capabilities.remove(PackageNamespace.PACKAGE_NAMESPACE);
		if ((packageCapabilities == null) || packageCapabilities.isEmpty()) {
			return;
		}
		if (packageCapabilities.stream()
			.anyMatch(cap -> cap.getAttributes()
				.containsKey(ClassIndexerAnalyzer.BND_HASHES))) {
			capabilities.put(PackageNamespace.PACKAGE_NAMESPACE, packageCapabilities);
			return;
		}

		Hierarchy index = supplier.get();
		for (Capability cap : packageCapabilities) {
			CapReqBuilder builder = CapReqBuilder.clone(cap);
			addHashes(index, cap, builder);
			addCapability(builder);
		}
	}

	private void addHashes(Hierarchy index, Capability cap, CapReqBuilder builder) {
		FolderNode resources = Optional.ofNullable((String) cap.getAttributes()
			.get(PackageNamespace.PACKAGE_NAMESPACE))
			.map(Descriptors::fqnToBinary)
			.flatMap(index::findFolder)
			.orElse(null);
		if (resources == null) {
			return;
		}

		List<Long> hashes = resources.stream()
			.map(NamedNode::name)
			.filter(Descriptors::isBinaryClass)
			.map(Descriptors::binaryToSimple)
			.distinct()
			.filter(simple -> !Verifier.isNumber(simple))
			.map(simple -> Long.valueOf(ClassIndexerAnalyzer.hash(simple)))
			.collect(toList());
		if (hashes.isEmpty()) {
			return;
		}

		builder.addAttribute(ClassIndexerAnalyzer.BND_HASHES, hashes);
	}

	public ResourceBuilder safeResourceBuilder() {
		return new SafeResourceBuilder();
	}

	private class SafeResourceBuilder extends ResourceBuilder {

		@Override
		public SupportingResource build() {
			return null;
		}

		@Override
		public ResourceBuilder addResource(Resource source) {
			return ResourceBuilder.this.addResource(source);
		}

		@Override
		public ResourceBuilder addCapability(Capability capability) {
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
		public ResourceBuilder addRequirement(Requirement requirement) {
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
		public boolean addManifest(Domain manifest) {
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			return ResourceBuilder.this.equals(obj);
		}

		@Override
		public void addExportServices(Parameters exportServices) {
			ResourceBuilder.this.addExportServices(exportServices);
		}

		@Override
		public void addImportServices(Parameters importServices) {
			ResourceBuilder.this.addImportServices(importServices);
		}

		@Override
		public RequirementBuilder getNativeCode(String header) {
			return ResourceBuilder.this.getNativeCode(header);
		}

		@Override
		public String toString() {
			return ResourceBuilder.this.toString();
		}

		@Override
		public void addRequireBundles(Parameters requireBundle) {
			ResourceBuilder.this.addRequireBundles(requireBundle);
		}

		@Override
		public void addRequireBundle(String bsn, VersionRange range) {
			ResourceBuilder.this.addRequireBundle(bsn, range);
		}

		@Override
		public void addRequireBundle(String bsn, Attrs attrs) {
			ResourceBuilder.this.addRequireBundle(bsn, attrs);
		}

		@Override
		public void addFragmentHost(String bsn, Attrs attrs) {
			ResourceBuilder.this.addFragmentHost(bsn, attrs);
		}

		@Override
		public void addRequireCapabilities(Parameters required) {
			ResourceBuilder.this.addRequireCapabilities(required);
		}

		@Override
		public void addRequireCapability(String namespace, String name, Attrs attrs) {
			ResourceBuilder.this.addRequireCapability(namespace, name, attrs);
		}

		@Override
		public List<Capability> addProvideCapabilities(Parameters capabilities) {
			return ResourceBuilder.this.addProvideCapabilities(capabilities);
		}

		@Override
		public List<Capability> addProvideCapabilities(String clauses) {
			return ResourceBuilder.this.addProvideCapabilities(clauses);
		}

		@Override
		public Capability addProvideCapability(String namespace, Attrs attrs) {
			return ResourceBuilder.this.addProvideCapability(namespace, attrs);
		}

		@Override
		public void addExportPackages(Parameters exports, String bundle_symbolic_name, Version bundle_version) {
			ResourceBuilder.this.addExportPackages(exports, bundle_symbolic_name, bundle_version);
		}

		@Override
		public void addExportPackages(Parameters exports) {
			ResourceBuilder.this.addExportPackages(exports);
		}

		@Override
		public void addEE(EE ee) {
			ResourceBuilder.this.addEE(ee);
		}

		@Override
		public void addExportPackage(String name, Attrs attrs, String bundle_symbolic_name, Version bundle_version) {
			ResourceBuilder.this.addExportPackage(name, attrs, bundle_symbolic_name, bundle_version);
		}

		@Override
		public void addExportPackage(String name, Attrs attrs) {
			ResourceBuilder.this.addExportPackage(name, attrs);
		}

		@Override
		public void addImportPackages(Parameters imports) {
			ResourceBuilder.this.addImportPackages(imports);
		}

		@Override
		public Requirement addImportPackage(String name, Attrs attrs) {
			return ResourceBuilder.this.addImportPackage(name, attrs);
		}

		@Override
		public void addExecutionEnvironment(EE ee) {
			ResourceBuilder.this.addExecutionEnvironment(ee);
		}

		@Override
		public void addAllExecutionEnvironments(EE ee) {
			ResourceBuilder.this.addAllExecutionEnvironments(ee);
		}

		@Override
		public void copyCapabilities(Set<String> ignoreNamespaces, Resource r) {
			ResourceBuilder.this.copyCapabilities(ignoreNamespaces, r);
		}

		@Override
		public void addCapabilities(List<Capability> capabilities) {
			ResourceBuilder.this.addCapabilities(capabilities);
		}

		@Override
		public void addRequirement(List<Requirement> requirements) {
			ResourceBuilder.this.addRequirement(requirements);
		}

		@Override
		public void addRequirements(List<Requirement> requires) {
			ResourceBuilder.this.addRequirements(requires);
		}

		@Override
		public List<Capability> findCapabilities(String ns, String filter) {
			return ResourceBuilder.this.findCapabilities(ns, filter);
		}

		@Override
		public Map<Capability, Capability> from(Resource bundle) {
			return ResourceBuilder.this.from(bundle);
		}

		@Override
		public Reporter getReporter() {
			return ResourceBuilder.this.getReporter();
		}

		@Override
		public void addContentCapability(URI uri, String sha256, long length, String mime) {
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
	public void addWorkspaceNamespace(String name) {
		// Add a capability specific to the workspace so that we can
		// identify this fact later during resource processing.
		CapabilityBuilder cap = new CapabilityBuilder(ResourceUtils.WORKSPACE_NAMESPACE);
		cap.addAttribute(ResourceUtils.WORKSPACE_NAMESPACE, name);
		addCapability(cap);
	}

	@Override
	public String toString() {
		return new StringBuilder("ResourceBuilder [caps=").append(capabilities)
			.append(", reqs=")
			.append(requirements)
			.append(']')
			.toString();
	}

	synchronized SupportingResource get() {
		if (!built)
			build();
		return resource;
	}

	/**
	 * Create a deferred resource builder so that any expensive actions are
	 * deferred until the supplier is called to get the resource.
	 *
	 * @param jar a Jar, preferably with checksum calculated, or null
	 * @param uri the uri to use or null (will use file uri as default)
	 * @param projectName if in a workspace, the project name or otherwise null
	 * @return a memo for creating the corresponding resource
	 */
	public static Supplier<Resource> memoize(Jar jar, URI uri, String projectName) throws Exception {

		assert jar != null : "jar is mandatory";
		assert jar.getSHA256()
			.isPresent() : "jar must have sha256";
		assert uri != null : "uri must be set";

		ResourceBuilder rb = new ResourceBuilder();
		boolean hasIdentity = rb.addJar(jar);

		byte[] digest = jar.getSHA256()
			.get();
		int length = jar.getLength();
		String mime = hasIdentity ? MIME_TYPE_BUNDLE : MIME_TYPE_JAR;
		String sha256 = Hex.toHexString(digest);
		rb.addContentCapability(uri, sha256, length, mime);
		if (projectName != null) {
			rb.addWorkspaceNamespace(projectName);
		}

		jar = null; // ensure jar not referenced from lambda

		return rb::get;
	}
}
