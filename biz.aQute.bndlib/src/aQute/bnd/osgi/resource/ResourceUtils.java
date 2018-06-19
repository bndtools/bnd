package aQute.bnd.osgi.resource;

import java.io.File;
import java.lang.reflect.InvocationHandler;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import org.osgi.service.repository.Repository;

import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.converter.Converter.Hook;
import aQute.lib.filter.Filter;
import aQute.lib.strings.Strings;

public class ResourceUtils {

	/**
	 * A comparator that compares the identity versions
	 */
	public static final Comparator<Resource>			IDENTITY_VERSION_COMPARATOR	= new Comparator<Resource>() {

																						@Override
																						public int compare(Resource o1,
																							Resource o2) {
																							if (o1 == o2)
																								return 0;

																							if (o1 == null)
																								return -1;

																							if (o2 == null)
																								return 1;

																							if (o1.equals(o2))
																								return 0;

																							String v1 = getIdentityVersion(
																								o1);
																							String v2 = getIdentityVersion(
																								o2);

																							if (v1 == v2)
																								return 0;

																							if (v1 == null)
																								return -1;

																							if (v2 == null)
																								return 1;

																							return new Version(v1)
																								.compareTo(
																									new Version(v2));
																						}

																					};

	private static final Comparator<? super Resource>	RESOURCE_COMPARATOR			= new Comparator<Resource>() {

																						@Override
																						public int compare(Resource o1,
																							Resource o2) {
																							if (o1 == o2)
																								return 0;

																							if (o1 == null)
																								return -1;
																							if (o2 == null)
																								return 1;

																							if (o1.equals(o2))
																								return 0;

																							if (o1 instanceof ResourceImpl
																								&& o2 instanceof ResourceImpl) {
																								return ((ResourceImpl) o1)
																									.compareTo(o2);
																							}

																							return o1.toString()
																								.compareTo(
																									o2.toString());
																						}
																					};

	public static final Resource						DUMMY_RESOURCE				= new ResourceBuilder().build();
	public static final String							WORKSPACE_NAMESPACE			= "bnd.workspace.project";

	static Converter									cnv							= new Converter();

	static {
		cnv.hook(Version.class, new Hook() {

			@Override
			public Object convert(java.lang.reflect.Type dest, Object o) throws Exception {
				if (o instanceof org.osgi.framework.Version)
					return new Version(o.toString());

				return null;
			}

		});
	}

	public static interface IdentityCapability extends Capability {
		public enum Type {
			bundle(IdentityNamespace.TYPE_BUNDLE),
			fragment(IdentityNamespace.TYPE_FRAGMENT),
			unknown(IdentityNamespace.TYPE_UNKNOWN),;
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

	public static ContentCapability getContentCapability(Resource resource) {
		List<ContentCapability> caps = getContentCapabilities(resource);
		if (caps.isEmpty())
			return null;
		return caps.get(0);
	}

	public static List<ContentCapability> getContentCapabilities(Resource resource) {
		List<ContentCapability> result = new ArrayList<>();

		for (Capability c : resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
			result.add(as(c, ContentCapability.class));
		}
		return result;
	}

	public static IdentityCapability getIdentityCapability(Resource resource) {
		List<Capability> caps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (caps.isEmpty())
			return null;

		return as(caps.get(0), IdentityCapability.class);
	}

	public static String getIdentityVersion(Resource resource) {
		IdentityCapability cap = getIdentityCapability(resource);
		if (cap == null)
			return null;

		Object v = cap.getAttributes()
			.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (v == null)
			return null;

		return v.toString();
	}

	public static BundleCap getBundleCapability(Resource resource) {
		List<Capability> caps = resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		if (caps.isEmpty())
			return null;

		return as(caps.get(0), BundleCap.class);
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

		if (v instanceof String) {
			if (!Version.isVersion((String) v))
				return null;

			return new Version((String) v);
		}

		return null;
	}

	public static final Version getVersion(Capability cap) {
		Object v = cap.getAttributes()
			.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (v == null)
			return null;

		if (v instanceof Version)
			return (Version) v;

		if (v instanceof org.osgi.framework.Version)
			return new Version(v.toString());

		if (v instanceof String)
			return Version.parseVersion((String) v);

		return null;
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

	public static String getVersionAttributeForNamespace(String ns) {
		String name;

		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns))
			name = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns))
			name = AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		else if (HostNamespace.HOST_NAMESPACE.equals(ns))
			name = AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		else if (PackageNamespace.PACKAGE_NAMESPACE.equals(ns))
			name = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else if (ServiceNamespace.SERVICE_NAMESPACE.equals(ns))
			name = null;
		else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(ns))
			name = ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else if (ExtenderNamespace.EXTENDER_NAMESPACE.equals(ns))
			name = ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else if (ContractNamespace.CONTRACT_NAMESPACE.equals(ns))
			name = ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else
			name = null;

		return name;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Capability> T as(final Capability cap, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
			type
		}, new InvocationHandler() {

			@Override
			public Object invoke(Object target, Method method, Object[] args) throws Throwable {
				if (Capability.class == method.getDeclaringClass())
					return method.invoke(cap, args);

				return get(method, cap.getAttributes(), cap.getDirectives(), args);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static <T extends Requirement> T as(final Requirement req, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {
			type
		}, new InvocationHandler() {

			@Override
			public Object invoke(Object target, Method method, Object[] args) throws Throwable {
				if (Requirement.class == method.getDeclaringClass())
					return method.invoke(req, args);

				return get(method, req.getAttributes(), req.getDirectives(), args);
			}
		});
	}

	@SuppressWarnings("unchecked")
	static <T> T get(Method method, Map<String, Object> attrs, Map<String, String> directives, Object[] args)
		throws Exception {
		String name = method.getName()
			.replace('_', '.');

		Object value = get0(name, attrs, directives, args);
		if (value == null) {
			value = get0(name.replace('_', '.'), attrs, directives, args);
		}

		if (value == null) {
			value = get0(name.replace('_', '-'), attrs, directives, args);
		}

		return (T) cnv.convert(method.getGenericReturnType(), value);
	}

	static Object get0(String name, Map<String, Object> attrs, Map<String, String> directives, Object[] args) {
		Object value;
		if (name.startsWith("$"))
			value = directives.get(name.substring(1));
		else {
			value = attrs.get(name);
		}
		if (value == null && args != null && args.length == 1)
			value = args[0];

		return value;
	}

	public static Set<Resource> getResources(Collection<? extends Capability> providers) {
		if (providers == null || providers.isEmpty())
			return Collections.emptySet();

		Set<Resource> resources = new TreeSet<>(RESOURCE_COMPARATOR);

		for (Capability c : providers) {
			resources.add(c.getResource());
		}

		return resources;
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

	public static boolean matches(Requirement r, Resource resource) {
		for (Capability c : resource.getCapabilities(r.getNamespace())) {
			if (matches(r, c))
				return true;
		}
		return false;
	}

	public static boolean matches(Requirement r, Capability c) {
		if (!r.getNamespace()
			.equals(c.getNamespace()))
			return false;

		if (!isEffective(r, c))
			return false;

		String filter = r.getDirectives()
			.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			return true;

		try {
			Filter f = new Filter(filter);
			return f.matchMap(c.getAttributes());
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

	public static ResolutionDirective getResolution(Requirement r) {
		String resolution = r.getDirectives()
			.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		if (resolution == null || resolution.equals(Namespace.RESOLUTION_MANDATORY))
			return ResolutionDirective.mandatory;

		if (resolution.equals(Namespace.RESOLUTION_OPTIONAL))
			return ResolutionDirective.optional;

		return null;
	}

	public static String toRequireCapability(Requirement req) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(req.getNamespace());

		CapReqBuilder r = new CapReqBuilder(req.getNamespace());
		r.addAttributes(req.getAttributes());
		r.addDirectives(req.getDirectives());
		Attrs attrs = r.toAttrs();
		sb.append(";")
			.append(attrs);
		return sb.toString();
	}

	public static String toProvideCapability(Capability cap) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(cap.getNamespace());

		CapReqBuilder r = new CapReqBuilder(cap.getNamespace());
		r.addAttributes(cap.getAttributes());
		r.addDirectives(cap.getDirectives());
		Attrs attrs = r.toAttrs();
		sb.append(";")
			.append(attrs);
		return sb.toString();
	}

	public static Map<URI, String> getLocations(Resource resource) {
		Map<URI, String> locations = new HashMap<>();
		for (ContentCapability c : getContentCapabilities(resource)) {
			URI uri = c.url();
			String sha = c.osgi_content();

			if (uri != null)
				locations.put(uri, sha);
		}
		return locations;
	}

	public static List<Capability> findProviders(Requirement requirement,
		Collection<? extends Capability> capabilities) {
		List<Capability> result = new ArrayList<>();
		for (Capability capability : capabilities)
			if (matches(requirement, capability))
				result.add(capability);
		return result;
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

	static <T> T requireNonNull(T obj) {
		if (obj != null) {
			return obj;
		}
		throw new NullPointerException();
	}

	static Collection<Requirement> all = Collections.singleton(createWildcardRequirement());

	/**
	 * Return all resources from a repository as returned by the wildcard
	 * requirement, see {@link #createWildcardRequirement()}
	 * 
	 * @param repository the repository to use
	 * @return a set of resources from the repository.
	 */
	public static Set<Resource> getAllResources(Repository repository) {
		Set<Capability> capabilities = repository.findProviders(all)
			.values()
			.stream()
			.flatMap(l -> l.stream())
			.collect(Collectors.toSet());
		return getResources(capabilities);

	}
}
