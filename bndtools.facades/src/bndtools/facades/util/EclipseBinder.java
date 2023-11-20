package bndtools.facades.util;

import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Status;
import org.osgi.annotation.versioning.ProviderType;

import biz.aQute.bnd.facade.api.Binder;

/**
 * This is a special extension point (EP) binder. It is used in scenarios where
 * there is an EP that needs a constant reference to an object. The EP should
 * instance an XFacade, where X is the type required by the EP's attribute. The
 * actual Binder ID (see FacadeManager FACADE_ID), is obtained via the
 * {@link IExecutableExtension}. The EP defines the class name in an attribute
 * and can add the facade ID after this class name, separating with a ':'. When
 * the class is instantiated by the EP manager, it will call the EclipseBinder
 * with extra information. For example:
 *
 * <pre>
 * <someep exec="com.example.IDo:bndtools.id.ido"> ... </someep>
 * </pre>
 *
 * In the previous case, the Facade ID will be `bndtools.id.ido`.
 * <p>
 * There are the following cases:
 * <ul>
 * <li>The object is defined by an interface. In that case the facade class can
 * extend the EclipseBinder. It will then automatically implement the
 * {@link IExecutableExtension}. when the
 * {@link IExecutableExtension#setInitializationData(IConfigurationElement, String, Object)}
 * is called, a {@link Binder} is automatically created.
 * <li>The executable object is a class type. In that case the facade class must
 * implement {@link IExecutableExtension} and delegate the
 * {@link IExecutableExtension#setInitializationData(IConfigurationElement, String, Object)}
 * to a new Binder.
 *
 * @param <D> the domain type
 */
@ProviderType
public class EclipseBinder<D> implements IExecutableExtension, Supplier<D> {

	final Class<D>	domainType;
	final Object	referent;

	Binder<D>		binder;

	/**
	 * Create a new Eclipse Binder.
	 *
	 * @param domainType the actual service type
	 * @param referent the object that refers to this binder. If null, this is
	 *            assumed.
	 */
	public EclipseBinder(Class<D> domainType, Object referent) {
		this.domainType = domainType;
		this.referent = referent == null ? this : referent;
	}

	/**
	 * Creates a Binder with the initialization data. The assumption is that the
	 * data parameter contains the string after the `:` the class name. This
	 * must be the full FACADE ID.
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {

		assert binder == null;

		if (!(data instanceof String)) {
			throw new CoreException(Status.error("must add the service id as parameter to the class name, like '"
				+ getClass().getName() + ":<service-id>'"));
		}

		binder = Binder.create(referent, domainType, (String) data, config.getName() + ":" + propertyName);
	}

	/**
	 * Return the current delegatee
	 */

	@Override
	public D get() {
		assert binder != null : "should not be called before setInitializationData has been done";
		return binder.get();
	}

}
