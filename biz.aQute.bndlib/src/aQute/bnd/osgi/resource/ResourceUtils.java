package aQute.bnd.osgi.resource;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.filter.Filter;
import aQute.lib.strings.Strings;

public class ResourceUtils {

	/**
	 * A comparator that compares the identity versions
	 */
	public static final Comparator<? super Resource>	IDENTITY_VERSION_COMPARATOR	=								//
		(o1, o2) -> {
			if (o1 == o2)
				return 0;

			if (o1 == null)
				return -1;

			if (o2 == null)
				return 1;

			if (o1.equals(o2))
				return 0;

			String v1 = getIdentityVersion(o1);
			String v2 = getIdentityVersion(o2);

			if (v1 == v2)
				return 0;

			if (v1 == null)
				return -1;

			if (v2 == null)
				return 1;

			return new Version(v1).compareTo(new Version(v2));
		};

	private static final Comparator<? super Resource>	RESOURCE_COMPARATOR			=								//
		(o1, o2) -> {
			if (o1 == o2)
				return 0;

			if (o1 == null)
				return -1;
			if (o2 == null)
				return 1;

			if (o1.equals(o2))
				return 0;

			if (o1 instanceof ResourceImpl && o2 instanceof ResourceImpl) {
				return ((ResourceImpl) o1).compareTo(o2);
			}

			return o1.toString()
				.compareTo(o2.toString());
		};

	public static final Resource						DUMMY_RESOURCE				= new ResourceBuilder().build();
	public static final String							WORKSPACE_NAMESPACE			= "bnd.workspace.project";

	private static final Converter						cnv							= new Converter()
		.hook(Version.class, (dest, o) -> toVersion(o));

	public interface IdentityCapability extends Capability {
		public enum Type {
			bundle(IdentityNamespace.TYPE_BUNDLE),
			fragment(IdentityNamespace.TYPE_FRAGMENT),
			unknown(IdentityNamespace.TYPE_UNKNOWN);
			private String s;

			private Type(String s) {
				this.s = s;
			}

			@Override
			public String toString() {
				return s;
			}

		}

		String osgi_identity();

		boolean singleton();

		Version version();

		Type type();

		URI uri();

		String copyright();

		String description(String string);

		String documentation();

		String license();
	}

	public interface ContentCapability extends Capability {
		String osgi_content();

		URI url();

		long size();

		String mime();
	}

	public interface BundleCap extends Capability {
		String osgi_wiring_bundle();

		boolean singleton();

		Version bundle_version();
	}

	private static Stream<Capability> capabilityStream(Resource resource, String namespace) {
		return resource.getCapabilities(namespace)
			.stream();
	}

	private static <T extends Capability> Stream<T> capabilityStream(Resource resource, String namespace,
		Class<T> type) {
		return capabilityStream(resource, namespace).map(c -> as(c, type));
	}

	public static ContentCapability getContentCapability(Resource resource) {
		return capabilityStream(resource, ContentNamespace.CONTENT_NAMESPACE, ContentCapability.class).findFirst()
			.orElse(null);
	}

	public static Optional<URI> getURI(Resource resource) {
		return capabilityStream(resource, ContentNamespace.CONTENT_NAMESPACE, ContentCapability.class).findFirst()
			.map(ContentCapability::url);
	}

	public static List<ContentCapability> getContentCapabilities(Resource resource) {
		return capabilityStream(resource, ContentNamespace.CONTENT_NAMESPACE, ContentCapability.class)
			.collect(toList());
	}

	public static IdentityCapability getIdentityCapability(Resource resource) {
		return capabilityStream(resource, IdentityNamespace.IDENTITY_NAMESPACE, IdentityCapability.class).findFirst()
			.orElse(null);
	}

	public static String getIdentityVersion(Resource resource) {
		return capabilityStream(resource, IdentityNamespace.IDENTITY_NAMESPACE, IdentityCapability.class).findFirst()
			.map(c -> c.getAttributes()
				.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE))
			.map(Object::toString)
			.orElse(null);
	}

	public static BundleCap getBundleCapability(Resource resource) {
		return capabilityStream(resource, BundleNamespace.BUNDLE_NAMESPACE, BundleCap.class).findFirst()
			.orElse(null);
	}

	public static Version toVersion(Object v) {
		if (v instanceof Version)
			return (Version) v;

		if (v instanceof org.osgi.framework.Version) {
			org.osgi.framework.Version o = (org.osgi.framework.Version) v;
			String q = o.getQualifier();
			return q.isEmpty() ? new Version(o.getMajor(), o.getMinor(), o.getMicro())
				: new Version(o.getMajor(), o.getMinor(), o.getMicro(), q);
		}

		if ((v instanceof String) && Version.isVersion((String) v)) {
			return Version.valueOf((String) v);
		}

		return null;
	}

	public static final Version getVersion(Capability cap) {
		String attr = getVersionAttributeForNamespace(cap.getNamespace());
		if (attr == null)
			return null;
		Object v = cap.getAttributes()
			.get(attr);
		return toVersion(v);
	}

	public static URI getURI(Capability contentCapability) {
		Object uriObj = contentCapability.getAttributes()
			.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
		if (uriObj == null)
			return null;

		if (uriObj instanceof URI)
			return (URI) uriObj;

		try {
			if (uriObj instanceof URL)
				return ((URL) uriObj).toURI();

			if (uriObj instanceof String) {
				try {
					URL url = new URL((String) uriObj);
					return url.toURI();
				} catch (MalformedURLException mfue) {
					// Ignore
				}

				File f = new File((String) uriObj);
				if (f.isFile()) {
					return f.toURI();
				}
				return new URI((String) uriObj);
			}

		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Resource content capability has illegal URL attribute", e);
		}

		return null;
	}

	public static String getVersionAttributeForNamespace(String namespace) {
		switch (namespace) {
			case IdentityNamespace.IDENTITY_NAMESPACE :
				return IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case BundleNamespace.BUNDLE_NAMESPACE :
			case HostNamespace.HOST_NAMESPACE :
				return AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
			case PackageNamespace.PACKAGE_NAMESPACE :
				return PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE :
				return ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case NativeNamespace.NATIVE_NAMESPACE :
				return NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE;
			case ExtenderNamespace.EXTENDER_NAMESPACE :
				return ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case ContractNamespace.CONTRACT_NAMESPACE :
				return ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case ImplementationNamespace.IMPLEMENTATION_NAMESPACE :
				return ImplementationNamespace.CAPABILITY_VERSION_ATTRIBUTE;
			case ServiceNamespace.SERVICE_NAMESPACE :
			default :
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Capability> T as(final Capability cap, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
			type
		}, (target, method, args) -> (Capability.class == method.getDeclaringClass()) ? publicLookup().unreflect(method)
			.bindTo(cap)
			.invokeWithArguments(args) : get(method, cap.getAttributes(), cap.getDirectives(), args));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Requirement> T as(final Requirement req, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
			type
		}, (target, method,
			args) -> (Requirement.class == method.getDeclaringClass()) ? publicLookup().unreflect(method)
				.bindTo(req)
				.invokeWithArguments(args) : get(method, req.getAttributes(), req.getDirectives(), args));
	}

	private static Object get(Method method, Map<String, Object> attrs, Map<String, String> directives, Object[] args)
		throws Exception {
		String name = method.getName()
			.replace('_', '.');

		Object value;
		if (name.startsWith("$"))
			value = directives.get(name.substring(1));
		else
			value = attrs.get(name);
		if (value == null && args != null && args.length == 1)
			value = args[0];

		return cnv.convert(method.getGenericReturnType(), value);
	}

	public static Set<Resource> getResources(Collection<? extends Capability> providers) {
		if (providers == null || providers.isEmpty())
			return Collections.emptySet();

		return getResources(providers.stream());
	}

	private static Set<Resource> getResources(Stream<? extends Capability> providers) {
		return providers.map(Capability::getResource)
			.collect(toCollection(() -> new TreeSet<>(RESOURCE_COMPARATOR)));
	}

	public static Requirement createWildcardRequirement() {
		return CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, "*", null)
			.buildSyntheticRequirement();
	}

	public static boolean isEffective(Requirement r, Capability c) {
		String capabilityEffective = c.getDirectives()
			.get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

		//
		// resolve on the capability will always match any
		// requirement effective
		//

		if (capabilityEffective == null) // default resolve
			return true;

		if (capabilityEffective.equals(Namespace.EFFECTIVE_RESOLVE))
			return true;

		String requirementEffective = r.getDirectives()
			.get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

		//
		// If requirement is resolve but capability isn't
		//

		if (requirementEffective == null)
			return false;

		return capabilityEffective.equals(requirementEffective);
	}

	public static boolean matches(Requirement requirement, Resource resource) {
		return capabilityStream(resource, requirement.getNamespace()).anyMatch(c -> matches(requirement, c));
	}

	public static boolean matches(Requirement requirement, Capability capability) {
		if (!requirement.getNamespace()
			.equals(capability.getNamespace()))
			return false;

		if (!isEffective(requirement, capability))
			return false;

		String filter = requirement.getDirectives()
			.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			return true;

		try {
			Filter f = new Filter(filter);
			return f.matchMap(capability.getAttributes());
		} catch (Exception e) {
			return false;
		}
	}

	public static String getEffective(Map<String, String> directives) {
		String effective = directives.get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
		if (effective == null)
			return Namespace.EFFECTIVE_RESOLVE;
		else
			return effective;
	}

	public static ResolutionDirective getResolution(Requirement requirement) {
		String resolution = requirement.getDirectives()
			.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		if (resolution == null || resolution.equals(Namespace.RESOLUTION_MANDATORY))
			return ResolutionDirective.mandatory;

		if (resolution.equals(Namespace.RESOLUTION_OPTIONAL))
			return ResolutionDirective.optional;

		return null;
	}

	public static String toRequireCapability(Requirement requirement) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(requirement.getNamespace());

		CapReqBuilder r = new CapReqBuilder(requirement.getNamespace());
		r.addAttributes(requirement.getAttributes());
		r.addDirectives(requirement.getDirectives());
		Attrs attrs = r.toAttrs();
		sb.append(";")
			.append(attrs);
		return sb.toString();
	}

	public static String toProvideCapability(Capability capability) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(capability.getNamespace());

		CapReqBuilder r = new CapReqBuilder(capability.getNamespace());
		r.addAttributes(capability.getAttributes());
		r.addDirectives(capability.getDirectives());
		Attrs attrs = r.toAttrs();
		sb.append(";")
			.append(attrs);
		return sb.toString();
	}

	public static Map<URI, String> getLocations(Resource resource) {
		return capabilityStream(resource, ContentNamespace.CONTENT_NAMESPACE, ContentCapability.class)
			.filter(c -> Objects.nonNull(c.url()))
			// We can't use Collectors.toMap since we must handle null
			// ContentCapability::osgi_content values.
			// .collect(toMap(ContentCapability::url,
			// ContentCapability::osgi_content));
			.collect(Collector.of(HashMap::new, (m, c) -> m.put(c.url(), c.osgi_content()), (m1, m2) -> {
				m1.putAll(m2);
				return m1;
			}));
	}

	public static List<Capability> findProviders(Requirement requirement,
		Collection<? extends Capability> capabilities) {
		return capabilities.stream()
			.filter(c -> matches(requirement, c))
			.collect(toList());
	}

	public static boolean isFragment(Resource resource) {
		IdentityCapability identity = getIdentityCapability(resource);
		if (identity == null)
			return false;
		return IdentityNamespace.TYPE_FRAGMENT.equals(identity.getAttributes()
			.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
	}

	public static String stripDirective(String name) {
		if (Strings.charAt(name, -1) == ':')
			return Strings.substring(name, 0, -1);
		return name;
	}

	public static String getIdentity(Capability identityCapability) throws IllegalArgumentException {
		String id = (String) identityCapability.getAttributes()
			.get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (id == null)
			throw new IllegalArgumentException("Resource identity capability has missing identity attribute");
		return id;
	}

	/**
	 * Create a VersionedClause by applying a version range mask to the
	 * resource! Masks are defined by
	 * {@link aQute.bnd.osgi.Macro#_range(String[])}. If the resource should
	 * represent a project in the bnd workspace, then instead the VersionClause
	 * will refer to it as a snapshot version: e.g. <bsn>;version=snapshot
	 */
	public static VersionedClause toVersionClause(Resource resource) {
		return toVersionClause(resource, "[===,==+)");
	}

	public static VersionedClause toVersionClause(Resource resource, String mask) {
		Capability idCap = getIdentityCapability(resource);
		String identity = getIdentity(idCap);
		String versionString;
		if (resource.getCapabilities(WORKSPACE_NAMESPACE)
			.isEmpty()) {
			Macro macro = new Macro(new Processor());
			Version version = getVersion(idCap);
			versionString = macro._range(new String[] {
				"range", mask, version.toString()
			});
		} else {
			versionString = "snapshot";
		}
		Attrs attribs = new Attrs();
		attribs.put(Constants.VERSION_ATTRIBUTE, versionString);
		return new VersionedClause(identity, attribs);
	}

	public static List<VersionedClause> toVersionedClauses(Collection<Resource> resources) {
		List<VersionedClause> runBundles = new ArrayList<>();
		for (Resource resource : resources) {
			VersionedClause runBundle = toVersionClause(resource);
			if (!runBundles.contains(runBundle)) {
				runBundles.add(runBundle);
			}
		}
		return runBundles;
	}

	private final static Collection<Requirement> all = Collections.singleton(createWildcardRequirement());

	/**
	 * Return all resources from a repository as returned by the wildcard
	 * requirement, see {@link #createWildcardRequirement()}
	 *
	 * @param repository the repository to use
	 * @return a set of resources from the repository.
	 */
	public static Set<Resource> getAllResources(Repository repository) {
		return getResources(repository.findProviders(all)
			.values()
			.stream()
			.flatMap(Collection::stream));
	}

	/**
	 * Compare two resources. This can be used to act as a comparator. The
	 * comparison is first done on name and then version.
	 *
	 * @param a the left resource
	 * @param b the right resource
	 * @return 0 if equal bame and version, 1 if left has a higher name or same
	 *         name and higher version, -1 otherwise
	 */
	public static int compareTo(Resource a, Resource b) {
		IdentityCapability left = ResourceUtils.getIdentityCapability(a);
		IdentityCapability right = ResourceUtils.getIdentityCapability(b);

		String myName = left.osgi_identity();
		String theirName = right.osgi_identity();
		if (myName == theirName)
			return 0;

		if (myName == null)
			return -1;

		if (theirName == null)
			return 1;

		int n = myName.compareTo(theirName);
		if (n != 0)
			return n;

		Version myVersion = left.version();
		Version theirVersion = right.version();

		if (myVersion == theirVersion)
			return 0;

		if (myVersion == null)
			return -1;

		if (theirVersion == null)
			return 1;

		return myVersion.compareTo(theirVersion);
	}

	public static List<Resource> sort(Collection<Resource> a) {
		List<Resource> list = new ArrayList<>(a);
		Collections.sort(list, ResourceUtils::compareTo);
		return list;
	}

	/**
	 * Sort the resources by symbolic name and version
	 *
	 * @param resources the set of resources to sort
	 * @return a sorted set of resources
	 */
	public static List<Resource> sortByNameVersion(Collection<Resource> resources) {
		ArrayList<Resource> sorted = new ArrayList<>(resources);
		Collections.sort(sorted, ResourceUtils::compareTo);
		return sorted;
	}

	public static boolean isInitialRequirement(Resource resource) {
		IdentityCapability identityCapability = getIdentityCapability(resource);
		if (identityCapability == null)
			return false;

		String osgi_identity = identityCapability.osgi_identity();
		if (osgi_identity == null)
			return false;

		return Constants.IDENTITY_INITIAL_RESOURCE.equals(osgi_identity);
	}

}
