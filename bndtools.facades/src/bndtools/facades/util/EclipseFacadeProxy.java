package bndtools.facades.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.libg.glob.Glob;
import aQute.libg.parameters.Attributes;
import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.FacadeManager;

/**
 * Implements a helper for facades that are interfaces. Using this model, you
 * can register an extension point with this class name. The parameters are then
 * passed after the class name, separated by a colon (':').
 *
 * <pre>
 * <someep
 *     class='bndtools.facades.util.EclipseFacadeProxy:id=my.facade,
 *          domain='ClasspathContainerInitializer', bundle=*'> ... </>
 * <pre>
 *
 * The attributes in the parameters are:
 * <ul>
 * <li><em>id</em> – The facade id, see {@link FacadeManager#FACADE_ID}
 * <li><em>domain</em> – The domain type, this is the type that the extension
 * point class must implement. This must be an interface type
 * <li><em>bundle</em> A glob for the bundle symbolic names that should be
 * searched for the domain type. This is optional, when not specified, it will
 * search all bundles.
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class EclipseFacadeProxy implements IExecutableExtensionFactory, IExecutableExtension {
	private static final String	ATTR_BUNDLE	= "bundle";
	private static final String	ATTR_DOMAIN	= "domain";
	private static final String	ATTR_ID		= "id";
	final static Logger			logger		= LoggerFactory.getLogger(EclipseFacadeProxy.class);
	static BundleContext		context;

	class ActiveFactory {
		final String				facadeId;
		final Class					facadeType;
		final IConfigurationElement	config;
		final String				propertyName;
		final Object				data;

		ActiveFactory(String facadeId, Class facadeType, IConfigurationElement config, String propertyName,
			Object data) {
			this.facadeId = facadeId;
			this.facadeType = facadeType;
			this.config = config;
			this.propertyName = propertyName;
			this.data = data;
		}

		Object create() {
			try {
				/*
				 * Forwarder class is because we need a constant reference to
				 * pass to the proxy as invocation handler but the bunder needs
				 * access to the facade to detect when it is gc'ed. Make sure
				 * nobody holds a reference to the forwarder.
				 */
				class Forwarder implements InvocationHandler {
					final Binder	binder;
					final Object	facade;

					Forwarder() throws CoreException {
						facade = Proxy.newProxyInstance(facadeType.getClassLoader(), new Class[] {
							facadeType
						}, this /* ! early escape */);
						binder = Binder.create(facade, facadeType, facadeId, "Eclipse proxy on " + facadeType);
						if (binder.get() instanceof IExecutableExtension iee) {
							iee.setInitializationData(config, propertyName, data);
						}
					}

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						assert binder != null;
						Object target = binder.get();
						return method.invoke(target, args);
					}
				}
				Forwarder f = new Forwarder();
				return f.facade;
			} catch (CoreException e) {
				fatal("unexpected exception %s", e);
				return null; // not called
			}
		}

	}

	ActiveFactory active;

	public EclipseFacadeProxy() {
		if (context == null) {
			Bundle bundle = FrameworkUtil.getBundle(EclipseFacadeProxy.class);
			if (bundle != null)
				context = bundle.getBundleContext();
		}
	}

	@Override
	public Object create() throws CoreException {
		if (active == null)
			fatal("proxy called but not yet initialized by " + IExecutableExtension.class.getSimpleName());

		return active.create();
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		try {
			String name = config.getName();

			Map<String, String> parameters = new Attributes((String) data);
			String id = parameters.get(ATTR_ID);
			String domain = parameters.get(ATTR_DOMAIN);
			String bundle = parameters.get(ATTR_BUNDLE);

			logger.debug("intitialize factory <{} {}='...:{}' ...>", name, propertyName, data);

			if (id == null)
				fatal("missing %s, EP <%s %s='%s' ... >", ATTR_ID, name, propertyName, data);
			if (domain == null)
				fatal("missing %s, EP <%s %s='%s' ... >", ATTR_DOMAIN, name, propertyName, data);

			Class domainType = findType(domain, bundle);

			this.active = new ActiveFactory(id, domainType, config, propertyName, data);
		} catch (Exception e) {
			fatal("failed to initialize " + e);
		}
	}

	private Class findType(String proxy, @Nullable
	String bundle) throws CoreException {

		assert proxy != null;

		Glob glob = Glob.ALL;
		if (bundle != null) {
			glob = new Glob(bundle);
		}

		List<Class> interfaces = new ArrayList<>();
		if (context != null) {
			for (Bundle b : context.getBundles()) {
				if (bundle == null) {
					String bsn = b.getSymbolicName();
					if (glob.matches(bsn)) {
						try {
							Class<?> proxyClass = b.loadClass(proxy);
							if (proxyClass.isInterface()) {
								interfaces.add(proxyClass);
							}
						} catch (ClassNotFoundException e) {
							// ignore
						}
					}
				}
			}
		} else {
			logger.warn("no bundle context");
			Class<?> proxyClass;
			try {
				proxyClass = Class.forName(proxy);
				if (proxyClass.isInterface()) {
					interfaces.add(proxyClass);
				}
			} catch (ClassNotFoundException e) {
				// ignore
			}
		}
		if (interfaces.isEmpty())
			fatal("cannot find proxy class " + proxy + " in bundles matching " + glob);
		logger.debug("found interfaces ", interfaces);
		return interfaces.get(0);
	}

	private void fatal(String message, Object... data) {
		String msg = String.format("""
			an %s is an %s. After its class name it expects a ':'
			and the parameters. The parameter are:

			id     ::= the facade id
			domain ::= <domain type class name>
			bundle ::= [optional] a glob of bundle symbolic names to load the domain type from

			parameters must be separated by a comma (',').
			""", EclipseFacadeProxy.class.getSimpleName(), IExecutableExtension.class.getName());

		String plain = String.format(message + "\n" + msg, data);

		logger.error(plain);

		throw Exceptions.duck(new CoreException(Status.error(plain)));
	}

}
