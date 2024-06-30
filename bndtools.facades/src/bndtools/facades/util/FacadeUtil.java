package bndtools.facades.util;

import java.util.Formatter;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.libg.parameters.Attributes;
import biz.aQute.bnd.facade.api.Binder;
import biz.aQute.bnd.facade.api.FacadeManager;

/**
 * Implements a helper for generated facades in Eclipse. This base class is an
 * {@link IExecutableExtensionFactory}. This means that after instantiation,
 * Eclipse will call {@link IExecutableExtensionFactory#create()} to get an
 * actual instance. Since we also implement {@link IExecutableExtension},we are
 * first called with the parameters, like the id of the service facade,
 * specified in the XML.
 *
 * <pre>
 * <some-ep class=
 * 'bndtools.facades.IAdapterFactoryFacade.java:id=bndtools.bnd.project.adapter,timeout=30'>
 * ... </some-ep>
 *
 * <pre>
 * The attributes in the parameters are:
 * <ul>
 * <li><em>id</em> – The facade id, see {@link FacadeManager#FACADE_ID}
 * <li><em>timeout</em> – The timeout in seconds the binding should wait for the
 * facade service
 * <li><em>description</em> – A description
 * </ul>
 */
@SuppressWarnings("rawtypes")
public abstract class FacadeUtil implements IExecutableExtensionFactory, IExecutableExtension {
	final static Logger			logger				= LoggerFactory.getLogger(FacadeUtil.class);
	public static final String	ATTR_ID				= "id";
	public static final String	ATTR_TIMEOUT		= "timeout";
	public static final String	ATTR_DESCRIPTION	= "description";

	/*
	 * Helper to maintain the data we get from the IExecutableExtension in one
	 * place
	 */
	class ActiveFactory {
		final String				facadeId;
		final IConfigurationElement	config;
		final String				propertyName;
		final Object				data;
		final Map<String, String>	parameters;
		final long					to;

		ActiveFactory(IConfigurationElement config, String propertyName, Object data) {
			Map<String, String> parameters = new Attributes((String) data);
			this.facadeId = parameters.get(ATTR_ID);
			String timeout = parameters.get(ATTR_TIMEOUT);

			if (facadeId == null)
				fatal("missing mandatory attribute %s in parameters", ATTR_ID);

			long to = -1;
			if (timeout != null && timeout.matches("\\d+")) {
				to = Long.parseLong(timeout) * 1000;
			}
			this.to = to;

			this.config = config;
			this.propertyName = propertyName;
			this.data = data;
			this.parameters = parameters;
		}

		Binder bind(Object facade) {
			Binder binder = Binder.create(facade, delegateType, facadeId, parameters);
			if (to > 0)
				binder.setTimeout(to);
			return binder;
		}

		@Override
		public String toString() {
			try (Formatter sb = new Formatter()) {

				IExtension extension = config.getDeclaringExtension();
				if (extension != null) {
					sb.format("ep=%s\n", extension.getLabel());
				}
				sb.format("<%s %s='%s:%s' ...>\n", config.getName(), propertyName, FacadeUtil.this.getClass()
					.getName(), data);

				return sb.toString();
			} catch (Exception e) {
				return e.toString();
			}
		}
	}

	final Class<?>	delegateType;
	ActiveFactory	active;

	/**
	 * Called by the subclass to provide the service under which the facade
	 * component will register.
	 *
	 * @param delegateServiceInterface the interface (not class) of the service
	 */
	public FacadeUtil(Class<?> delegateServiceInterface) {
		assert delegateServiceInterface.isInterface();
		this.delegateType = delegateServiceInterface;
	}

	/**
	 * Create an instance of the facade that is bound to the designated delegate
	 */
	@Override
	public Object create() throws CoreException {
		assert active != null;
		return createFacade(active::bind);
	}

	/**
	 * parse the initialization data
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		assert active == null : "one time initialization expected";
		try {
			String name = config.getName();
			this.active = new ActiveFactory(config, propertyName, data);
			logger.debug("intitialize factory %s", this.active);
		} catch (Exception e) {
			fatal("failed to initialize " + e);
		}
	}

	protected abstract Object createFacade(Function<Object, Supplier<Object>> bindFunction);

	private void fatal(String message, Object... data) {
		String msg = String.format("""
			Failed extension point with facade class %s.
			""", this.getClass());

		if (active != null)
			msg = "\n" + active.toString();

		String plain = String.format(message + "\n" + msg, data);
		logger.error(plain);

		throw Exceptions.duck(new CoreException(Status.error(plain)));
	}

}
