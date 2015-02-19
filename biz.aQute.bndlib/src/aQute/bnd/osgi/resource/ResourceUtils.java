package aQute.bnd.osgi.resource;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;

import org.osgi.framework.namespace.*;
import org.osgi.namespace.contract.*;
import org.osgi.namespace.extender.*;
import org.osgi.namespace.service.*;
import org.osgi.resource.*;
import org.osgi.service.repository.*;

import aQute.bnd.version.*;
import aQute.lib.converter.*;
import aQute.lib.converter.Converter.Hook;

public class ResourceUtils {
	static Converter	cnv	= new Converter();
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
			bundle(IdentityNamespace.TYPE_BUNDLE), fragment(IdentityNamespace.TYPE_FRAGMENT), unknown(
					IdentityNamespace.TYPE_UNKNOWN), ;
			private String	s;

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
		URI url();

		long size();

		String mime();
	}

	public static ContentCapability getContentCapability(Resource resource) {
		List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
		if (caps == null || caps.isEmpty())
			return null;

		return as(caps.get(0), ContentCapability.class);
	}

	public static IdentityCapability getIdentityCapability(Resource resource) {
		List<Capability> caps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (caps == null || caps.isEmpty())
			return null;

		return as(caps.get(0), IdentityCapability.class);
	}

	public static final Version getVersion(Capability cap) {
		Object v = cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
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
		Object uriObj = contentCapability.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
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
				}
				catch (MalformedURLException mfue) {
					// Ignore
				}

				File f = new File((String) uriObj);
				if (f.isFile()) {
					return f.toURI();
				}
				return new URI((String) uriObj);
			}

		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Resource content capability has illegal URL attribute", e);
		}

		return null;
	}

	public static String getVersionAttributeForNamespace(String ns) {
		String name;

		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns))
			name = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		else if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns))
			name = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		else if (HostNamespace.HOST_NAMESPACE.equals(ns))
			name = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
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
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class< ? >[] {
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
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class< ? >[] {
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
	static <T> T get(Method method, Map<String,Object> attrs, Map<String,String> directives, Object[] args)
			throws Exception {
		String name = method.getName().replace('_', '.');

		Object value;
		if (name.startsWith("$"))
			value = directives.get(name.substring(1));
		else
			value = attrs.get(name);
		if (value == null && args != null && args.length == 1)
			value = args[0];

		return (T) cnv.convert(method.getGenericReturnType(), value);
	}
}
